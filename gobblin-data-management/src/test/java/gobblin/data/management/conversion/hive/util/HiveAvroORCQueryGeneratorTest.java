/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package gobblin.data.management.conversion.hive.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.avro.Schema;
import org.apache.hadoop.hive.metastore.api.Table;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

import gobblin.data.management.ConversionHiveTestUtils;
import gobblin.data.management.conversion.hive.query.HiveAvroORCQueryGenerator;
import gobblin.util.AvroFlattener;


@Slf4j
@Test(groups = { "gobblin.data.management.conversion" })
public class HiveAvroORCQueryGeneratorTest {

  private static String resourceDir = "avroToOrcQueryUtilsTest";
  private static Optional<Table> destinationTableMeta = Optional.absent();
  private static boolean isEvolutionEnabled = true;
  private static Optional<Integer> rowLimit = Optional.absent();

  /***
   * Test DDL generation for schema structured as: Array within record within array within record
   * @throws IOException
   */
  @Test
  public void testArrayWithinRecordWithinArrayWithinRecordDDL() throws IOException {
    String schemaName = "testArrayWithinRecordWithinArrayWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "arrayWithinRecordWithinArrayWithinRecord_nested.json");

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(schema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());

    Assert.assertEquals(q,
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "arrayWithinRecordWithinArrayWithinRecord_nested.ddl"));
  }

  /***
   * Test DDL generation for schema structured as: option within option within record
   * @throws IOException
   */
  @Test
  public void testOptionWithinOptionWithinRecordDDL() throws IOException {
    String schemaName = "testOptionWithinOptionWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "optionWithinOptionWithinRecord_nested.json");

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(schema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());

    Assert.assertEquals(q,
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "optionWithinOptionWithinRecord_nested.ddl"));
  }

  /***
   * Test DDL generation for schema structured as: record within option within record
   * @throws IOException
   */
  @Test
  public void testRecordWithinOptionWithinRecordDDL() throws IOException {
    String schemaName = "testRecordWithinOptionWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "recordWithinOptionWithinRecord_nested.json");

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(schema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());

    Assert.assertEquals(q.trim(),
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "recordWithinOptionWithinRecord_nested.ddl"));
  }

  /***
   * Test DDL generation for schema structured as: record within record within record
   * @throws IOException
   */
  @Test
  public void testRecordWithinRecordWithinRecordDDL() throws IOException {
    String schemaName = "testRecordWithinRecordWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "recordWithinRecordWithinRecord_nested.json");

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(schema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());

    Assert.assertEquals(q.trim(),
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "recordWithinRecordWithinRecord_nested.ddl"));
  }

  /***
   * Test DDL generation for schema structured as: record within record within record after flattening
   * @throws IOException
   */
  @Test
  public void testRecordWithinRecordWithinRecordFlattenedDDL() throws IOException {
    String schemaName = "testRecordWithinRecordWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir, "recordWithinRecordWithinRecord_nested.json");

    AvroFlattener avroFlattener = new AvroFlattener();
    Schema flattenedSchema = avroFlattener.flatten(schema, true);

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(flattenedSchema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());

    Assert.assertEquals(q,
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "recordWithinRecordWithinRecord_flattened.ddl"));
  }

  /***
   * Test DML generation
   * @throws IOException
   */
  @Test
  public void testRecordWithinRecordWithinRecordFlattenedDML() throws IOException {
    String schemaName = "testRecordWithinRecordWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "recordWithinRecordWithinRecord_nested.json");

    AvroFlattener avroFlattener = new AvroFlattener();
    Schema flattenedSchema = avroFlattener.flatten(schema, true);

    String q = HiveAvroORCQueryGenerator
        .generateTableMappingDML(schema, flattenedSchema, schemaName, schemaName + "_orc", Optional.<String>absent(),
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<Boolean>absent(),
            Optional.<Boolean>absent(), isEvolutionEnabled, destinationTableMeta, rowLimit);

    Assert.assertEquals(q.trim(),
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "recordWithinRecordWithinRecord.dml"));
  }

  /***
   * Test bad schema
   * @throws IOException
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNonRecordRootSchemaDDL() throws Exception {
    String schemaName = "nonRecordRootSchema";
    Schema schema = Schema.create(Schema.Type.STRING);

    String q = HiveAvroORCQueryGenerator
        .generateCreateTableDDL(schema, schemaName, "file:/user/hive/warehouse/" + schemaName,
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<List<String>>absent(),
            Optional.<Map<String, HiveAvroORCQueryGenerator.COLUMN_SORT_ORDER>>absent(), Optional.<Integer>absent(),
            Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
            Optional.<Map<String, String>>absent(), isEvolutionEnabled, destinationTableMeta,
            new HashMap<String, String>());
  }

  /***
   * Test DML generation with row limit
   * @throws IOException
   */
  @Test
  public void testFlattenedDMLWithRowLimit() throws IOException {
    String schemaName = "testRecordWithinRecordWithinRecordDDL";
    Schema schema = ConversionHiveTestUtils.readSchemaFromJsonFile(resourceDir,
        "recordWithinRecordWithinRecord_nested.json");
    Optional<Integer> rowLimit = Optional.of(1);

    AvroFlattener avroFlattener = new AvroFlattener();
    Schema flattenedSchema = avroFlattener.flatten(schema, true);

    String q = HiveAvroORCQueryGenerator
        .generateTableMappingDML(schema, flattenedSchema, schemaName, schemaName + "_orc", Optional.<String>absent(),
            Optional.<String>absent(), Optional.<Map<String, String>>absent(), Optional.<Boolean>absent(),
            Optional.<Boolean>absent(), isEvolutionEnabled, destinationTableMeta, rowLimit);

    Assert.assertEquals(q.trim(),
        ConversionHiveTestUtils.readQueryFromFile(resourceDir, "flattenedWithRowLimit.dml"));
  }
}
