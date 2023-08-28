package com.benjamintanone.springdatajpademojava.controllers;

import com.benjamintanone.springdatajpademojava.domain.Customer;
import com.benjamintanone.springdatajpademojava.repositories.CustomerRepository;
import com.benjamintanone.springdatajpademojava.service.CustomerService;
import com.benjamintanone.springdatajpademojava.specifications.CustomerSpecification;
import org.aspectj.weaver.patterns.HasThisTypePatternTriedToSneakInSomeGenericOrParameterizedTypePatternMatchingStuffAnywhereVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomerService customerService;

    private String getCsvRowFromCustomer(Customer customer) {
        return String.format("%d,%s\n", customer.getId(), customer.getName());
    }

    private void setCsvParams(final HttpServletResponse response) {
        // not important; basically sets csv params so clients can understand it's a csv
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=customers.csv");
    }

    private void writeCustomersToResponseAsCsv(Stream<Customer> customerStream,
                                final HttpServletResponse response) throws IOException {
        setCsvParams(response);
        PrintWriter printWriter = response.getWriter();
        printWriter.write("id,name\n"); // the CSV column header, not really important here
        customerStream.peek(customer -> printWriter.write(getCsvRowFromCustomer(customer)))
            .forEach(entityManager::detach); // optional, but objects _may_ not be GC'd if you don't detach them first.
        printWriter.flush();
        printWriter.close();
    }

    @GetMapping("/customers_fragment.csv")
    @Transactional(readOnly = true) // this is important, because Streams can only be opened in a transaction
    public ResponseEntity<StreamingResponseBody> getCustomersCsv(
            final HttpServletResponse response,
            @RequestParam(required = false) final String name
    ) throws IOException {
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=customers.csv");
        StreamingResponseBody stream = outputStream -> {
            customerService.exportStreamToCsv(outputStream, name);
        };
        return ResponseEntity.ok(stream);
    }

    @GetMapping("/customers_page_by_page.csv")
    public void getCustomersCsvPageByPage(
            final HttpServletResponse response,
            @RequestParam(required = false) final String name
    ) throws IOException {
        setCsvParams(response);
        PrintWriter printWriter = response.getWriter();
        Specification<Customer> specification = CustomerSpecification.hasName(name);
        Page<Customer> customerPage;
        int page = 0;
        do {
            customerPage = customerRepository.findAll(specification, PageRequest.of(page, 10));
            customerPage.getContent()
                    .forEach(customer -> printWriter.write(getCsvRowFromCustomer(customer)));
            page++;
        } while (customerPage.hasNext());
    }

    @GetMapping("/customer_findall")
    public String getCustomersCsvPageByPage(final HttpServletResponse response) {
        setCsvParams(response);
        Iterable<Customer> customers = customerRepository.findAll(CustomerSpecification.hasName(""));
        return StreamSupport.stream(customers.spliterator(), false)
                .map(customer -> String.format("%d,%s", customer.getId(), customer.getName()))
                .collect(Collectors.joining("\n"));
    }
}
