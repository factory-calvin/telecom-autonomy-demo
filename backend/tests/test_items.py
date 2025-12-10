def test_read_root(client):
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "message": "API is running"}


def test_list_items_empty(client):
    response = client.get("/api/items")
    assert response.status_code == 200
    assert response.json() == []


def test_create_item(client):
    response = client.post(
        "/api/items", json={"name": "Test Item", "description": "A test item"}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "Test Item"
    assert data["description"] == "A test item"
    assert "id" in data


def test_get_item(client):
    # Create an item first
    create_response = client.post("/api/items", json={"name": "Get Test"})
    item_id = create_response.json()["id"]

    # Get the item
    response = client.get(f"/api/items/{item_id}")
    assert response.status_code == 200
    assert response.json()["name"] == "Get Test"


def test_get_item_not_found(client):
    response = client.get("/api/items/999")
    assert response.status_code == 404


def test_update_item(client):
    # Create an item first
    create_response = client.post("/api/items", json={"name": "Original"})
    item_id = create_response.json()["id"]

    # Update the item
    response = client.put(
        f"/api/items/{item_id}", json={"name": "Updated", "description": "New desc"}
    )
    assert response.status_code == 200
    assert response.json()["name"] == "Updated"
    assert response.json()["description"] == "New desc"


def test_delete_item(client):
    # Create an item first
    create_response = client.post("/api/items", json={"name": "To Delete"})
    item_id = create_response.json()["id"]

    # Delete the item
    response = client.delete(f"/api/items/{item_id}")
    assert response.status_code == 200
    assert response.json() == {"ok": True}

    # Verify it's gone
    get_response = client.get(f"/api/items/{item_id}")
    assert get_response.status_code == 404
