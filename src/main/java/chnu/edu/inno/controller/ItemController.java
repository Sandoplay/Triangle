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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/items")
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable String id) {
        return itemService.getItemById(id);
    }

    @GetMapping()
    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }

    @PostMapping
    public Item createItem(@RequestBody Item item) {
        return itemService.createItem(item);
    }

    @PutMapping
    public Item updateItem(@RequestBody Item item) {
        return itemService.updateItem(item);
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable String id) {
        itemService.deleteItemById(id);
    }

    //----------------  More ---------------------------

    @GetMapping("/response/{id}")
    public ApiResponse<BaseMetaData, Item> getItemByIdAsApiResponse(@PathVariable String id) {
        return itemService.getItemByIdAsApiResponse(id);
    }

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
