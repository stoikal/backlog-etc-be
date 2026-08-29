package com.stoikal.backlog_etc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ListPageResponse {
    private final List<ListDto> data;
    private final Pagination pagination;

    public ListPageResponse(List<ListDto> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<ListDto> getData() { return data; }
    public Pagination getPagination() { return pagination; }

    public static class Pagination {
        private final int page;
        private final int limit;
        private final long total;
        @JsonProperty("total_pages")
        private final int totalPages;

        public Pagination(int page, int limit, long total, int totalPages) {
            this.page = page;
            this.limit = limit;
            this.total = total;
            this.totalPages = totalPages;
        }

        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public long getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
}