package de.fdpg.dataportal_backend.dse.api;

import de.fdpg.dataportal_backend.common.api.DisplayEntry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DseProfileTreeNodeTest {

  @Test
  void testConstructorWithValues() {
    DseProfileTreeNode childNode = DseProfileTreeNode.builder()
        .id("childid")
        .children(null)
        .name(null)
        .display(null)
        .fields(null)
        .module(null)
        .url(null)
        .leaf(false)
        .selectable(false)
        .build();

    DseProfileTreeNode parentNode = DseProfileTreeNode.builder()
        .id("id")
        .children(List.of(childNode))
        .name("name")
        .display(DisplayEntry.builder().build())
        .fields(FieldDisplayEntry.builder().build())
        .module("module")
        .url("http://url")
        .leaf(true)
        .selectable(true)
        .build();

    assertNotNull(parentNode);
    assertNotNull(childNode);
    assertNotEquals(Collections.emptyList(), parentNode.children());
    assertEquals(childNode.id(), parentNode.children().get(0).id());
  }

  @Test
  public void testConstructorWithNull() {
    DseProfileTreeNode dseProfileTreeNode = DseProfileTreeNode.builder()
        .id(null)
        .children(null)
        .name(null)
        .display(null)
        .fields(null)
        .module(null)
        .url(null)
        .leaf(false)
        .selectable(false)
        .build();

    assertNotNull(dseProfileTreeNode);
    assertNotNull(dseProfileTreeNode.children());
    assertEquals(Collections.emptyList(), dseProfileTreeNode.children());
  }
}
