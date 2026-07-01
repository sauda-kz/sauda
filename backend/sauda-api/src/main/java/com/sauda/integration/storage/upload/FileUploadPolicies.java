package com.sauda.integration.storage.upload;

import java.util.Map;
import java.util.Set;

public final class FileUploadPolicies {

    public static final FileUploadPolicy RAW_DISTRIBUTOR_PRICE =
            new FileUploadPolicy(
                    Set.of("xlsx", "xls", "csv"),
                    Map.of(
                            "xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "xls",
                            "application/vnd.ms-excel",
                            "csv",
                            "text/csv"),
                    "raw distributor price");

    public static final FileUploadPolicy LOT_ATTACHMENT =
            new FileUploadPolicy(
                    Set.of("pdf", "doc", "docx", "png", "jpg", "jpeg"),
                    Map.of(
                            "pdf",
                            "application/pdf",
                            "doc",
                            "application/msword",
                            "docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "png",
                            "image/png",
                            "jpg",
                            "image/jpeg",
                            "jpeg",
                            "image/jpeg"),
                    "lot attachment");

    private FileUploadPolicies() {}
}
