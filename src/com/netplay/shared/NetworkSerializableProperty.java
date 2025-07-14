package com.netplay.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface NetworkSerializableProperty {
  /**
   * The serialization order index. Methods are serialized in ascending order by this value.
   * @return the zero-based index for serialization order
   */
  int value();
}
