/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.eval.app;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Options;

import org.apache.tika.eval.app.db.ColInfo;
import org.apache.tika.eval.app.db.Cols;
import org.apache.tika.eval.app.db.TableInfo;
import org.apache.tika.eval.app.io.ExtractReader;
import org.apache.tika.eval.app.io.ExtractReaderException;
import org.apache.tika.eval.app.io.IDBWriter;
import org.apache.tika.eval.core.util.ContentTags;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.pipes.fetcher.FetchKey;

public class ExtractProfiler extends ProfilerBase {

    private final static String FIELD = "f";
    public static TableInfo EXTRACT_EXCEPTION_TABLE =
            new TableInfo("extract_exceptions", new ColInfo(Cols.CONTAINER_ID, Types.INTEGER), new ColInfo(Cols.FILE_PATH, Types.VARCHAR, FILE_PATH_MAX_LEN),
                    new ColInfo(Cols.EXTRACT_EXCEPTION_ID, Types.INTEGER), new ColInfo(Cols.PARSE_ERROR_ID, Types.INTEGER));
    public static TableInfo EXCEPTION_TABLE =
            new TableInfo("parse_exceptions", new ColInfo(Cols.ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.ORIG_STACK_TRACE, Types.VARCHAR, 8192),
                    new ColInfo(Cols.SORT_STACK_TRACE, Types.VARCHAR, 8192), new ColInfo(Cols.PARSE_EXCEPTION_ID, Types.INTEGER));
    public static TableInfo CONTAINER_TABLE =
            new TableInfo("containers", new ColInfo(Cols.CONTAINER_ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.FILE_PATH, Types.VARCHAR, FILE_PATH_MAX_LEN),
                    new ColInfo(Cols.LENGTH, Types.BIGINT), new ColInfo(Cols.EXTRACT_FILE_LENGTH, Types.BIGINT));
    public static TableInfo PROFILE_TABLE = new TableInfo("profiles", new ColInfo(Cols.ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.CONTAINER_ID, Types.INTEGER),
            new ColInfo(Cols.FILE_NAME, Types.VARCHAR, 256), new ColInfo(Cols.MD5, Types.CHAR, 32), new ColInfo(Cols.LENGTH, Types.BIGINT),
            new ColInfo(Cols.IS_EMBEDDED, Types.BOOLEAN), new ColInfo(Cols.EMBEDDED_DEPTH, Types.INTEGER), new ColInfo(Cols.EMBEDDED_FILE_PATH, Types.VARCHAR, 1024),
            new ColInfo(Cols.ATTACHMENT_TYPE, Types.VARCHAR, 32), new ColInfo(Cols.FILE_EXTENSION, Types.VARCHAR, 12), new ColInfo(Cols.MIME_ID, Types.INTEGER),
            new ColInfo(Cols.ELAPSED_TIME_MILLIS, Types.INTEGER), new ColInfo(Cols.NUM_ATTACHMENTS, Types.INTEGER), new ColInfo(Cols.NUM_METADATA_VALUES, Types.INTEGER),
            new ColInfo(Cols.NUM_PAGES, Types.INTEGER), new ColInfo(Cols.NUM_OCR_PAGES, Types.INTEGER), new ColInfo(Cols.HAS_CONTENT, Types.BOOLEAN));
    public static TableInfo EMBEDDED_FILE_PATH_TABLE =
            new TableInfo("emb_file_names", new ColInfo(Cols.ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.EMBEDDED_FILE_PATH, Types.VARCHAR, 1024));
    public static TableInfo CONTENTS_TABLE = new TableInfo("contents", new ColInfo(Cols.ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.CONTENT_LENGTH, Types.INTEGER),
            new ColInfo(Cols.NUM_UNIQUE_TOKENS, Types.INTEGER), new ColInfo(Cols.NUM_TOKENS, Types.INTEGER), new ColInfo(Cols.COMMON_TOKENS_LANG, Types.VARCHAR, 12),
            new ColInfo(Cols.NUM_UNIQUE_COMMON_TOKENS, Types.INTEGER), new ColInfo(Cols.NUM_COMMON_TOKENS, Types.INTEGER),
            new ColInfo(Cols.NUM_UNIQUE_ALPHABETIC_TOKENS, Types.INTEGER), new ColInfo(Cols.NUM_ALPHABETIC_TOKENS, Types.INTEGER), new ColInfo(Cols.OOV, Types.DOUBLE),
            new ColInfo(Cols.TOP_N_TOKENS, Types.VARCHAR, 1024), new ColInfo(Cols.LANG_ID_1, Types.VARCHAR, 12), new ColInfo(Cols.LANG_ID_PROB_1, Types.FLOAT),
            new ColInfo(Cols.LANG_ID_2, Types.VARCHAR, 12), new ColInfo(Cols.LANG_ID_PROB_2, Types.FLOAT), new ColInfo(Cols.UNICODE_CHAR_BLOCKS, Types.VARCHAR, 1024),
            new ColInfo(Cols.TOKEN_ENTROPY_RATE, Types.FLOAT), new ColInfo(Cols.TOKEN_LENGTH_SUM, Types.INTEGER), new ColInfo(Cols.TOKEN_LENGTH_MEAN, Types.FLOAT),
            new ColInfo(Cols.TOKEN_LENGTH_STD_DEV, Types.FLOAT), new ColInfo(Cols.CONTENT_TRUNCATED_AT_MAX_LEN, Types.BOOLEAN));
    public static TableInfo TAGS_TABLE =
            new TableInfo("tags", new ColInfo(Cols.ID, Types.INTEGER, "PRIMARY KEY"), new ColInfo(Cols.TAGS_A, Types.INTEGER), new ColInfo(Cols.TAGS_B, Types.INTEGER),
                    new ColInfo(Cols.TAGS_DIV, Types.INTEGER), new ColInfo(Cols.TAGS_I, Types.INTEGER), new ColInfo(Cols.TAGS_IMG, Types.INTEGER),
                    new ColInfo(Cols.TAGS_LI, Types.INTEGER), new ColInfo(Cols.TAGS_OL, Types.INTEGER), new ColInfo(Cols.TAGS_P, Types.INTEGER),
                    new ColInfo(Cols.TAGS_TABLE, Types.INTEGER), new ColInfo(Cols.TAGS_TD, Types.INTEGER), new ColInfo(Cols.TAGS_TITLE, Types.INTEGER),
                    new ColInfo(Cols.TAGS_TR, Types.INTEGER), new ColInfo(Cols.TAGS_U, Types.INTEGER), new ColInfo(Cols.TAGS_UL, Types.INTEGER),
                    new ColInfo(Cols.TAGS_PARSE_EXCEPTION, Types.BOOLEAN));
    static Options OPTIONS;

    private final Path inputDir;
    private final Path extracts;
    private final ExtractReader extractReader;


    ExtractProfiler(Path inputDir, Path extracts, ExtractReader extractReader, IDBWriter dbWriter) {
        super(dbWriter);
        this.inputDir = inputDir;
        this.extracts = extracts;
        this.extractReader = extractReader;
    }


    @Override
    public boolean processFileResource(FetchKey fetchKey) {
        EvalFilePaths fps = null;

        if (inputDir != null && inputDir.equals(extracts)) {
            //crawling an extract dir
            fps = getPathsFromExtractCrawl(fetchKey, extracts);
        } else {
            fps = getPathsFromSrcCrawl(fetchKey, inputDir, extracts);
        }
        int containerId = ID.incrementAndGet();
        String containerIdString = Integer.toString(containerId);

        ExtractReaderException.TYPE extractExceptionType = null;

        List<Metadata> metadataList = null;
        try {
            metadataList = extractReader.loadExtract(fps.getExtractFile());
        } catch (ExtractReaderException e) {
            extractExceptionType = e.getType();
        }

        Map<Cols, String> contOutput = new HashMap<>();
        long srcFileLen = getSourceFileLength(fps, metadataList);
        contOutput.put(Cols.LENGTH, srcFileLen > NON_EXISTENT_FILE_LENGTH ? Long.toString(srcFileLen) : "");
        contOutput.put(Cols.CONTAINER_ID, containerIdString);
        contOutput.put(Cols.FILE_PATH, fps
                .getRelativeSourceFilePath()
                .toString());

        if (fps.getExtractFileLength() > 0) {
            contOutput.put(Cols.EXTRACT_FILE_LENGTH, (fps.getExtractFile() == null) ? "" : Long.toString(fps.getExtractFileLength()));
        }
        try {
            writer.writeRow(CONTAINER_TABLE, contOutput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        if (extractExceptionType != null) {
            try {
                writeExtractException(EXTRACT_EXCEPTION_TABLE, containerIdString, fps
                        .getRelativeSourceFilePath()
                        .toString(), extractExceptionType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        List<Integer> numAttachments = countAttachments(metadataList);
        int i = 0;
        for (Metadata m : metadataList) {
            ContentTags contentTags = getContent(fps, m);
            //the first file should have the same id as the container id
            String fileId = (i == 0) ? containerIdString : Integer.toString(ID.incrementAndGet());
            writeTagData(fileId, contentTags, TAGS_TABLE);
            writeProfileData(fps, i, contentTags, m, fileId, containerIdString, numAttachments, PROFILE_TABLE);
            writeEmbeddedPathData(i, fileId, m, EMBEDDED_FILE_PATH_TABLE);
            writeExceptionData(fileId, m, EXCEPTION_TABLE);
            try {
                Map<Class, Object> textStats = calcTextStats(contentTags);
                writeContentData(fileId, textStats, CONTENTS_TABLE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            i++;
        }
        return true;
    }


    private void writeEmbeddedPathData(int i, String fileId, Metadata m, TableInfo embeddedFilePathTable) {
        if (i == 0) {
            return;
        }
        Map<Cols, String> data = new HashMap<>();
        data.put(Cols.ID, fileId);
        data.put(Cols.EMBEDDED_FILE_PATH, m.get(TikaCoreProperties.EMBEDDED_RESOURCE_PATH));
        try {
            writer.writeRow(embeddedFilePathTable, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
