package com.example.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;

import java.util.Collection;

@Data
//@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SearchResults extends PagedResources<Author> {
    //did this just to support test!
    public SearchResults(Collection<Author> content, PageMetadata metadata, Iterable<Link> links) {
        super(content, metadata, links);
    }

    @JsonProperty("openSearch")
    @Override
    public PageMetadata getMetadata() {
        return super.getMetadata();
    }
}
