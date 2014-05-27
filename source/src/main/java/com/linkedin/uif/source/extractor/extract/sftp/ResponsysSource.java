package com.linkedin.uif.source.extractor.extract.sftp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.linkedin.uif.configuration.ConfigurationKeys;
import com.linkedin.uif.configuration.SourceState;
import com.linkedin.uif.configuration.WorkUnitState;
import com.linkedin.uif.configuration.WorkUnitState.WorkingState;
import com.linkedin.uif.source.Source;
import com.linkedin.uif.source.extractor.Extractor;
import com.linkedin.uif.source.extractor.exception.ExtractPrepareException;
import com.linkedin.uif.source.extractor.extract.Command;
import com.linkedin.uif.source.extractor.extract.CommandOutput;
import com.linkedin.uif.source.extractor.extract.sftp.SftpCommand.SftpCommandType;
import com.linkedin.uif.source.workunit.Extract;
import com.linkedin.uif.source.workunit.WorkUnit;
import com.linkedin.uif.source.workunit.Extract.TableType;

/**
 * Source class for Responsys data, responsible for querying Responsys
 * in order to get a list of files to pull for this current run. It then
 * distributes the files among the work units
 * @author stakiar
 */
public class ResponsysSource implements Source<String, String>
{  
    private static final Logger log = LoggerFactory.getLogger(ResponsysSource.class);
    
    private ChannelSftp sftp;
    private SourceState sourceState;
    
    public void initLogger(SourceState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(Strings.nullToEmpty(state.getProp(ConfigurationKeys.SOURCE_SCHEMA)));
        sb.append("_");
        sb.append(Strings.nullToEmpty(state.getProp(ConfigurationKeys.SOURCE_ENTITY)));
        sb.append("]");
        MDC.put("tableName", sb.toString());
    }
    
    @Override
    public Extractor<String, String> getExtractor(WorkUnitState state) throws IOException {
        Extractor<String, String> extractor = null;
        try {
            extractor = new ResponsysExtractor<String, String>(state).build();
        } catch (ExtractPrepareException e) {
            throw new IOException("Failed to prepare extractor: error -" + e.getMessage(), e);
        }
        return extractor;
    }

    /**
     * This method takes the snapshot seen in the previous run, and compares it to the list
     * of files currently in Responsys - it then decided which files it needs to pull
     * and distributes those files across the workunits
     */
    @Override
    public List<WorkUnit> getWorkunits(SourceState state)
    {
        initLogger(state);
        this.sourceState = state;
        this.sftp = (ChannelSftp) SftpExecutor.connect(state.getProp(ConfigurationKeys.SOURCE_PRIVATE_KEY),
                                                       state.getProp(ConfigurationKeys.SOURCE_KNOWN_HOSTS),
                                                       state.getProp(ConfigurationKeys.SOURCE_USERNAME),
                                                       state.getProp(ConfigurationKeys.SOURCE_HOST_NAME),
                                                       state.getProp(ConfigurationKeys.SOURCE_USE_PROXY_URL),
                                                       state.getPropAsInt(ConfigurationKeys.SOURCE_USE_PROXY_PORT, -1));
        
        log.info("Get work units");
        List<WorkUnit> workUnits = Lists.newArrayList();
        String nameSpaceName = state.getProp(ConfigurationKeys.EXTRACT_NAMESPACE_NAME_KEY);
        String entityName = state.getProp(ConfigurationKeys.SOURCE_ENTITY);
        
        // Override extract table name
        String extractTableName = state.getProp(ConfigurationKeys.EXTRACT_TABLE_NAME_KEY);
        
        // If extract table name is not found then consider entity name as extract table name
        if(Strings.isNullOrEmpty(extractTableName)) {
            extractTableName = entityName;
        }

        TableType tableType = TableType.valueOf(state.getProp(ConfigurationKeys.EXTRACT_TABLE_TYPE_KEY).toUpperCase());        
        List<WorkUnitState> previousWorkunits = state.getPreviousStates();
        List<String> prevFsSnapshot = new ArrayList<String>();
        
        // Get list of files seen in the previous run
        if (!previousWorkunits.isEmpty() && previousWorkunits.get(0).getWorkunit().contains(ConfigurationKeys.SOURCE_RESPONSYS_FS_SNAPSHOT)) {
            prevFsSnapshot = previousWorkunits.get(0).getWorkunit().getPropAsList(ConfigurationKeys.SOURCE_RESPONSYS_FS_SNAPSHOT);
        }

        // Get list of files that need to be pulled
        List<String> currentFsSnapshot = this.getcurrentFsSnapshot();
        List<String> filesToPull = new ArrayList<String>(currentFsSnapshot);
        filesToPull.removeAll(prevFsSnapshot);
        log.info("Will pull the following files in this run: " + Arrays.toString(filesToPull.toArray()));

        int numPartitions = state.contains((ConfigurationKeys.SOURCE_MAX_NUMBER_OF_PARTITIONS)) &&
                            state.getPropAsInt(ConfigurationKeys.SOURCE_MAX_NUMBER_OF_PARTITIONS) <= filesToPull.size() ?
                            state.getPropAsInt(ConfigurationKeys.SOURCE_MAX_NUMBER_OF_PARTITIONS) : filesToPull.size();
        int filesPerPartition = (numPartitions == 0) ? 0 : (int) Math.ceil(filesToPull.size() / numPartitions);
        int workUnitCount = 0;
        int fileOffset = 0;
        
        // Distribute the files across the workunits
        for (int i = 0; i < numPartitions; i++) {
            SourceState partitionState = new SourceState();
            partitionState.addAll(state);
            partitionState.setProp(ConfigurationKeys.SOURCE_RESPONSYS_FS_SNAPSHOT, StringUtils.join(currentFsSnapshot, ","));
            partitionState.setProp(ConfigurationKeys.SOURCE_FILES_TO_PULL, StringUtils.join(filesToPull.subList(fileOffset, fileOffset + filesPerPartition > filesToPull.size() ? filesToPull.size() : fileOffset + filesPerPartition), ","));
            partitionState.setProp(ConfigurationKeys.WORK_UNIT_LOW_WATER_MARK_KEY, -1);
            partitionState.setProp(ConfigurationKeys.WORK_UNIT_HIGH_WATER_MARK_KEY, -1);
            
            // Use extract table name to create extract
            Extract extract = partitionState.createExtract(tableType, nameSpaceName, extractTableName);
            workUnits.add(partitionState.createWorkUnit(extract));
            workUnitCount++;
            fileOffset += filesPerPartition;
        }
        
        log.info("Total number of work units for the current run: " + workUnitCount);
        
        List<WorkUnit> previousWorkUnits = this.getPreviousIncompleteWorkUnits(state);
        log.info("Total number of work units from the previous failed runs: " + previousWorkUnits.size());
        
        workUnits.addAll(previousWorkUnits);
        return workUnits;
    }

    /**
     * Get all the previous work units which are in incomplete state
     *
     * @param SourceState
     * @return list of work units
     */
    private List<WorkUnit> getPreviousIncompleteWorkUnits(SourceState state) {
        log.debug("Getting previous unsuccessful work units");
        List<WorkUnit> previousWorkUnits = new ArrayList<WorkUnit>();
        List<WorkUnitState> previousWorkUnitStates = state.getPreviousStates();
        if(previousWorkUnitStates.size() == 0) {
            log.debug("Previous states are not found");
            return previousWorkUnits;
        }
        
        for(WorkUnitState workUnitState : previousWorkUnitStates) {
            if(workUnitState.getWorkingState() == WorkingState.FAILED || workUnitState.getWorkingState() == WorkingState.ABORTED) {
                previousWorkUnits.add(workUnitState.getWorkunit());
            }
        }
        
        return previousWorkUnits;
    }

    /**
     * Connects to the source and does on ls on the directory where the data is located,
     * looking for files with the pattern *SOURCE_ENTITY*
     * @return list of file names matching the specified pattern
     */
    private List<String> getcurrentFsSnapshot()
    {
        List<Command> cmds = SftpExecutor.parseInputCommands(sourceState.getProp(ConfigurationKeys.SOURCE_DATA_COMMANDS));
        List<String> list = Arrays.asList("*" + sourceState.getProp(ConfigurationKeys.SOURCE_ENTITY) + "*");
        cmds.add(new SftpCommand().withCommandType(SftpCommandType.LS).withParams(list));
        CommandOutput<SftpCommand, List<String>> response = new SftpCommandOutput();
        
        try
        {
            response = SftpExecutor.executeUnixCommands(cmds, this.sftp);
        }
        catch (SftpException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        catch (SftpCommandFormatException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }

        Map<SftpCommand, List<String>> results = response.getResults();
        for (Map.Entry<SftpCommand, List<String>> entry : results.entrySet()) {
            if (entry.getKey().getCommandType().equals(SftpCommandType.LS)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    @Override
    public void shutdown(SourceState state)
    {
        sftp.disconnect();
    }
}