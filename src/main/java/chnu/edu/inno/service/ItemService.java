package chnu.edu.inno.service;


/*
  @author   george
  @project   inno
  @class  ItemService
  @version  1.0.0 
  @since 27/09/2026 - 19.05
*/

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @PostConstruct
    public void init() {
        itemRepository.deleteAll();
     List<Item> items = List.of(
             new Item("Milk", "000001", 1),
             new Item("Bread", "00002", 2),
             new Item("Sugar", "00003", 3),
             new Item("Salt", "00004", 1),
             new Item("Meat", "00005", 1),
             new Item("Chips", "00006", 3),
             new Item("Coca-Cola", "00007", 1),
             new Item("Hot dog", "00008", 1)
     );
     itemRepository.saveAll(items);
    }

    public ApiResponse<BaseMetaData, Item> getAllItems() {
        return new ApiResponse<>(new BaseMetaData(200, true), itemRepository.findAll());
    }

    public ApiResponse<BaseMetaData, Item> getItemById(String id) {
        return itemRepository.findById(id)
                .map(item -> new ApiResponse<>(new BaseMetaData(200, true), item))
                .orElseGet(() -> notFound(id));
    }

    public ApiResponse<BaseMetaData, Item> createItem(Item item) {
        if (item.getId() != null && itemRepository.existsById(item.getId())) {
            return new ApiResponse<>(new BaseMetaData(409, false, "Item with id " + item.getId() + " already exists"));
        }
        if (item.getName() == null || item.getName().isBlank()) {
            return badRequest("Name must not be empty");
        }
        if (item.getQuantity() < 0) {
            return badRequest("Quantity cannot be negative");
        }
        return new ApiResponse<>(new BaseMetaData(201, true), itemRepository.save(item));
    }

    public ApiResponse<BaseMetaData, Item> updateItem(Item item) {
        if (item.getId() == null || !itemRepository.existsById(item.getId())) {
            return notFound(item.getId());
        }
        if (item.getName() == null || item.getName().isBlank()) {
            return badRequest("Name must not be empty");
        }
        if (item.getQuantity() < 0) {
            return badRequest("Quantity cannot be negative");
        }
        return new ApiResponse<>(new BaseMetaData(200, true), itemRepository.save(item));
    }

    public ApiResponse<BaseMetaData, Item> deleteItemById(String id) {
        if (!itemRepository.existsById(id)) {
            return notFound(id);
        }
        itemRepository.deleteById(id);
        return new ApiResponse<>(new BaseMetaData(200, true));
    }

    private ApiResponse<BaseMetaData, Item> notFound(String id) {
        return new ApiResponse<>(new BaseMetaData(404, false, "Item with id " + id + " not found"));
    }

    private ApiResponse<BaseMetaData, Item> badRequest(String message) {
        return new ApiResponse<>(new BaseMetaData(400, false, message));
    }

    public ApiResponse<PaginationMetaData, Item> getPage(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Item> items = itemRepository.findAll(pageable);

        PaginationMetaData metaData = new PaginationMetaData();

        metaData.setNumber(items.getNumber());
        metaData.setSize(items.getSize());
        metaData.setTotalElements(items.getTotalElements());
        metaData.setTotalPages(items.getTotalPages());
        metaData.setFirst(items.isFirst());
        metaData.setLast(items.isLast());
        List<Item> list = items.getContent();
        ApiResponse<PaginationMetaData, Item> response = new ApiResponse<>(metaData,list);
        return response;
    }
}
