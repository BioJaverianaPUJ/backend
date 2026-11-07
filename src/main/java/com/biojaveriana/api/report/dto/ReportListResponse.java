package com.biojaveriana.api.report.dto;

import com.biojaveriana.api.common.Pagination;

import java.util.List;

public record ReportListResponse(
    List<ReportSummaryResponse> content,
    Pagination pagination
) {}
