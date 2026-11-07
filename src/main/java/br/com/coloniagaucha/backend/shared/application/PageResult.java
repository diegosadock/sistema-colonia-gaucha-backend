package br.com.coloniagaucha.backend.shared.application;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size, int totalPages) {
    public PageResult { items = List.copyOf(items); }
}
