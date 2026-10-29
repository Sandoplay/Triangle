package chnu.edu.inno.service;


/*
  @author   george
  @project   inno
  @class  ItemService
  @version  1.0.0 
  @since 27/09/2026 - 19.05
*/

import chnu.edu.inno.exception.ItemAlreadyExistsException;
import chnu.edu.inno.exception.ItemNotFoundException;
import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.javers.spring.annotation.JaversAuditable;
import org.javers.spring.annotation.JaversAuditableDelete;
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
        Item item = itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        return new ApiResponse<>(new BaseMetaData(200, true), item);
    }

    // JaVers commits the argument only when the method returns normally,
    // so rejected requests (exceptions) never get into the audit
    @JaversAuditable
    public ApiResponse<BaseMetaData, Item> createItem(Item item) {
        if (item.getId() != null && itemRepository.existsById(item.getId())) {
            throw new ItemAlreadyExistsException(item.getId());
        }
        validate(item);
        return new ApiResponse<>(new BaseMetaData(201, true), itemRepository.save(item));
    }

    @JaversAuditable
    public ApiResponse<BaseMetaData, Item> updateItem(Item item) {
        Item existing = itemRepository.findById(item.getId())
                .orElseThrow(() -> new ItemNotFoundException(item.getId()));
        validate(item);
        // request body has no audit fields, keep the original creation info
        item.setCreatedDate(existing.getCreatedDate());
        item.setCreatedBy(existing.getCreatedBy());
        return new ApiResponse<>(new BaseMetaData(200, true), itemRepository.save(item));
    }

    @JaversAuditableDelete(entity = Item.class)
    public ApiResponse<BaseMetaData, Item> deleteItemById(String id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        itemRepository.deleteById(id);
        return new ApiResponse<>(new BaseMetaData(200, true));
    }

    private void validate(Item item) {
        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        if (item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
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
