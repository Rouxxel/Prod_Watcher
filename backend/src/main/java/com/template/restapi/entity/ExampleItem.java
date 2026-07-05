/**
 * #############################################################################
 * ### Example entity
 * ###
 * ### @file ExampleItem.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * The entity as persisted in the data source, including the server-managed
 * `id`.
 *
 * In Java backends the persisted shape (entity) is deliberately kept separate
 * from the request bodies (dto/*Create, *Update) and the API output
 * (dto/*Response). Replace/rename this class per project; keep the separation.
 *
 * If you add JPA later, annotate this with @Entity / @Id and let a real
 * repository manage it.
 */
package com.template.restapi.entity;

public class ExampleItem {

    private String id;            // server-generated unique identifier
    private String name;          // domain field (shared by every variant)
    private String description;   // optional free-text description

    public ExampleItem() {
    }

    public ExampleItem(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
