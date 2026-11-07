package com.biojaveriana.api.report.dto;

import com.biojaveriana.api.common.Pagination;

public record ReportListResponse(
    java.util.List<ReportSummaryResponse> content,
    Pagination pagination
) {}
