package com.benjamintanone.springdatajpademojava.service;

import com.benjamintanone.springdatajpademojava.domain.Customer;
import com.benjamintanone.springdatajpademojava.repositories.CustomerRepository;
import com.benjamintanone.springdatajpademojava.specifications.CustomerSpecification;
import com.benjamintanone.springdatajpademojava.utils.CSVWriterWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.io.OutputStream;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;

    private static final String[] headerNames = new String[2];

    static {
        headerNames[0] = "id";
        headerNames[1] = "name";
    }

    @Override
    public void exportStreamToCsv(OutputStream outputStream, String name) {
        Specification<Customer> specification = CustomerSpecification.hasName(name);
        Stream<Customer> customerStream = customerRepository.stream(specification, Customer.class);
        try (var csvWriter = new CSVWriterWrapper(outputStream)) {
            csvWriter.writeNext(headerNames);
            customerStream.peek(d -> csvWriter.writeNext(toStringArray(d)))
                            .forEach(entityManager::detach);
            csvWriter.flush();
        }
    }

    private String[] toStringArray(Customer customer) {
        String[] stringData = new String[2];
        stringData[0] = customer.getId().toString();
        stringData[1] = customer.getName();
        return stringData;
    }
}
