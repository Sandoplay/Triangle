package chnu.edu.inno.controller;


/*
  @author   Sandoplay
  @project   inno
  @class  AuditController
  @version  1.0.0
  @since 29/10/2026
*/

import chnu.edu.inno.model.Item;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping(value = "api/v1/audit/items", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditController {

    private final Javers javers;

    @GetMapping
    @Operation(summary = "Audit by all items: changes of every Item")
    public String getAllChanges() {
        return javers.getJsonConverter().toJson(
                javers.findChanges(QueryBuilder.byClass(Item.class).build()));
    }

    @GetMapping("/{id}/changes")
    @Operation(summary = "Changes of one item: InitialValueChange, ValueChange, ...")
    public String getChanges(@PathVariable String id) {
        return javers.getJsonConverter().toJson(
                javers.findChanges(QueryBuilder.byInstanceId(id, Item.class).build()));
    }

    @GetMapping("/{id}/snapshots")
    @Operation(summary = "Snapshots of one item with type INITIAL / UPDATE / TERMINAL")
    public String getSnapshots(@PathVariable String id) {
        return javers.getJsonConverter().toJson(
                javers.findSnapshots(QueryBuilder.byInstanceId(id, Item.class).build()));
    }

    @GetMapping("/{id}/shadows")
    @Operation(summary = "Shadows by id: full historical states of one item")
    public List<Map<String, Object>> getShadows(@PathVariable String id) {
        List<Shadow<Item>> shadows = javers.findShadows(QueryBuilder.byInstanceId(id, Item.class).build());
        return shadows.stream().map(shadow -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("commitId", shadow.getCommitMetadata().getId().value());
            result.put("author", shadow.getCommitMetadata().getAuthor());
            result.put("commitDate", shadow.getCommitMetadata().getCommitDate().toString());
            result.put("item", shadow.get());
            return result;
        }).toList();
    }
}
