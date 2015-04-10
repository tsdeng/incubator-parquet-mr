package parquet.io;

import parquet.io.GroupColumnIO;
import parquet.io.MessageColumnIO;
import parquet.io.PrimitiveColumnIO;
import parquet.schema.*;

import java.util.ArrayList;
import java.util.List;

public class MyColumnIOFactory {
  /**
   * visit a parquet schema and build a columnIO
   */
  public class ColumnIOCeatorVisitor implements TypeVisitor {

    /** root IO to return */
    private MessageColumnIO columnIO;
    private GroupColumnIO current;
    private List<PrimitiveColumnIO> leaves = new ArrayList<PrimitiveColumnIO>();
    private final boolean validating;//for what?
    private final MessageType requestedSchema;
    private int currentRequestedIndex;
    private Type currentRequestedType;
    private boolean stricTypeChecking;

    public ColumnIOCeatorVisitor(boolean validating, MessageType requestedSchema, boolean strictTypeChecking){
      this.validating = validating;
      this.requestedSchema = requestedSchema;
      this.stricTypeChecking = strictTypeChecking;
    }

    /**
     * visit root
     * @param messageType the message type to visit
     */
    @Override
    public void visit(MessageType messageType) {
      columnIO = new MessageColumnIO(requestedSchema, validating);

    }

    @Override
    public void visit(GroupType groupType) {

    }



    @Override
    public void visit(PrimitiveType primitiveType) {

    }
  }
}
