package chnu.edu.inno.exception;


/*
  @author   Sandoplay
  @project   inno
  @class  ItemNotFoundException
  @version  1.0.0
  @since 29/10/2026
*/

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String id) {
        super("Item with id " + id + " not found");
    }
}
