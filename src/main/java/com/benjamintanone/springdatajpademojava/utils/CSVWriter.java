package com.benjamintanone.springdatajpademojava.utils;

public interface CSVWriter extends AutoCloseable {

    void writeNext(String[] nextLine);

    void flush();

}
