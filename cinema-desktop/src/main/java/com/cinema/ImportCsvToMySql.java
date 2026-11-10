package com.cinema;

import repository.StorageIO;
import java.nio.file.Path;

/** One-time import utility. Back up CSV and MySQL before running. */
public final class ImportCsvToMySql {
    public static void main(String[] args) throws Exception {
        if (!StorageIO.mysqlEnabled()) {
            throw new IllegalStateException("Set CINEMA_STORAGE=mysql before import");
        }
        Path path = Path.of(args.length == 0 ? "database" : args[0]);
        StorageIO.importAllCsv(path);
        System.out.println("CSV import completed. Check all 9 tables before using the app.");
    }
}
