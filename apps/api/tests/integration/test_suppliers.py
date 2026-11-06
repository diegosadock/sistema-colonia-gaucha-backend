import uuid


def new_cnpj() -> str:
    return str(uuid.uuid4().int)[:14]  # único a cada execução


def test_create_supplier_ok(client):
    r = client.post("/api/v1/suppliers", json={"name": "Distribuidora Sul", "document": new_cnpj()})
    assert r.status_code == 201
    assert r.json()["is_active"] is True


def test_create_supplier_invalid_name(client):
    r = client.post("/api/v1/suppliers", json={"name": "A"})
    assert r.status_code == 422


def test_duplicate_document(client):
    doc = new_cnpj()
    client.post("/api/v1/suppliers", json={"name": "Fornecedor 1", "document": doc})
    r = client.post("/api/v1/suppliers", json={"name": "Fornecedor 2", "document": doc})
    assert r.status_code == 409


def test_get_not_found(client):
    assert client.get("/api/v1/suppliers/999999").status_code == 404


def test_deactivate_supplier(client):
    created = client.post("/api/v1/suppliers", json={"name": "Para Desativar"}).json()
    r = client.patch(f"/api/v1/suppliers/{created['id']}", json={"is_active": False})
    assert r.status_code == 200 and r.json()["is_active"] is False
    # persistiu?
    assert client.get(f"/api/v1/suppliers/{created['id']}").json()["is_active"] is False