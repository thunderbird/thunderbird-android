package org.apache.james.mime4j.dom.field;

import java.util.Date;

public interface DateTimeField extends ParsedField {

    Date getDate();

}