/**
 * #############################################################################
 * ### Example repository (in-memory)
 * ###
 * ### @file ExampleItemRepository.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Thin persistence layer. This template ships an in-memory implementation
 * (a ConcurrentHashMap) so it runs with zero external dependencies.
 *
 * Swap this for a real data source when needed:
 *   - JPA: make it an interface `extends JpaRepository<ExampleItem, String>`
 *     and delete the body below.
 *   - JSON file store: delegate to util/SecureFileIo's record helpers
 *     (createRecord/readAll/updateRecord/deleteRecord) against a file under
 *     src/main/resources/db/mock_db_jsons/.
 */
package com.template.restapi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.template.restapi.entity.ExampleItem;

import org.springframework.stereotype.Repository;

@Repository
public class ExampleItemRepository {

    private final ConcurrentHashMap<String, ExampleItem> store = new ConcurrentHashMap<>();

    public List<ExampleItem> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<ExampleItem> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public ExampleItem save(ExampleItem item) {
        store.put(item.getId(), item);
        return item;
    }

    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }
}
