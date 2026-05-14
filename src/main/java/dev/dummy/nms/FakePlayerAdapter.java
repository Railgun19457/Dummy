package dev.dummy.nms;

import dev.dummy.dummy.DummyCreateRequest;

public interface FakePlayerAdapter {
    DummyHandle spawn(DummyCreateRequest request);
}
