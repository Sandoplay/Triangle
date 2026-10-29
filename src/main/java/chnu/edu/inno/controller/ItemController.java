package chnu.edu.inno.controller;


/*
  @author   george
  @project   inno
  @class  ItemController
  @version  1.0.0 
  @since 27/09/2026 - 19.09
*/

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/items")
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    @Operation(summary = "Get item by id", description = "Returns the item wrapped in ApiResponse, or meta with code 404")
    public ResponseEntity<ApiResponse<BaseMetaData, Item>> getItemById(@PathVariable String id) {
        return toResponseEntity(itemService.getItemById(id));
    }

    @GetMapping()
    @Operation(summary = "Get all items")
    public ResponseEntity<ApiResponse<BaseMetaData, Item>> getAllItems() {
        return toResponseEntity(itemService.getAllItems());
    }

    @PostMapping
    @Operation(summary = "Create item", description = "Returns 201 with created item, 400 on invalid data, 409 if id exists")
    public ResponseEntity<ApiResponse<BaseMetaData, Item>> createItem(@RequestBody Item item) {
        return toResponseEntity(itemService.createItem(item));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update item", description = "Returns 200 with updated item, 400 on invalid data, 404 if not found")
    public ResponseEntity<ApiResponse<BaseMetaData, Item>> updateItem(@PathVariable String id, @RequestBody Item item) {
        item.setId(id);
        return toResponseEntity(itemService.updateItem(item));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete item by id", description = "Returns 200 on success, 404 if not found")
    public ResponseEntity<ApiResponse<BaseMetaData, Item>> deleteItem(@PathVariable String id) {
        return toResponseEntity(itemService.deleteItemById(id));
    }

    private <M extends BaseMetaData> ResponseEntity<ApiResponse<M, Item>> toResponseEntity(ApiResponse<M, Item> response) {
        return ResponseEntity.status(response.getMeta().getCode()).body(response);
    }

    //----------------  More ---------------------------

    @GetMapping("/page")
    @Operation(
            method = "GET",
            summary = "Get  ALL items, ASC, PAGEABLE. Default 5 items per page",
            description = "Fetch a PAGE of items  by page number and size "
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Success. Return page of items",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            type = "object",
                                            properties = {
                                                    @StringToClassMapItem(key = "meta", value = PaginationMetaData.class),
                                                    @StringToClassMapItem(key = "data", value = Item.class)
                                            }
                                    )
                            )
                    }
            )
    })
    public ApiResponse<PaginationMetaData, Item> getItemsPage(
            @Parameter(description = "Request parameter. Page of paginated result. Cannot be less than zero")
            @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Request parameter. Size of paginated page result. Cannot be less than zero")
            @RequestParam(name = "size", defaultValue = "5", required = false) Integer size
    ) {
        return itemService.getPage(page, size);
    }





}
