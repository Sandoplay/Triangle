package chnu.edu.inno.exception;


/*
  @author   Sandoplay
  @project   inno
  @class  ItemAlreadyExistsException
  @version  1.0.0
  @since 29/10/2026
*/

public class ItemAlreadyExistsException extends RuntimeException {
    public ItemAlreadyExistsException(String id) {
        super("Item with id " + id + " already exists");
    }
}
