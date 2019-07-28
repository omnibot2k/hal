package com.example.hal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.ResourceSupport;

@Dat:qa
@NoArgsConstructor
@AllArgsConstructor
public class Author extends ResourceSupport {
    String firstName;
    String lastName;
}
