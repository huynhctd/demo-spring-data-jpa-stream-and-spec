package com.benjamintanone.springdatajpademojava.service;

import java.io.OutputStream;

public interface CustomerService {
    void exportStreamToCsv(OutputStream outputStream, String name);
}
