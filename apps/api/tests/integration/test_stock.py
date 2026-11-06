import pytest


@pytest.fixture
def product(client):
    # ajuste o payload se o Diego mudar os campos do produto
    r = client.post(
        "/api/v1/products",
        json={"name": "Tomate", "base_unit": "kg", "category": "Hortifruti"},
    )
    assert r.status_code == 201
    return r.json()


def move(client, product_id, type_, qty, reason=None):
    return client.post(
        "/api/v1/stock/movements",
        json={"product_id": product_id, "type": type_, "quantity": qty, "reason": reason},
    )


def balance_of(client, product_id) -> float:
    r = client.get("/api/v1/stock/balances", params={"product_id": product_id})
    return float(r.json()[0]["balance"])


def test_entry_increases_balance(client, product):
    assert move(client, product["id"], "ENTRY", "10").status_code == 201
    assert balance_of(client, product["id"]) == 10


def test_exit_decreases_balance(client, product):
    move(client, product["id"], "ENTRY", "10")
    assert move(client, product["id"], "EXIT", "4").status_code == 201
    assert balance_of(client, product["id"]) == 6


def test_exit_above_balance_is_refused(client, product):
    move(client, product["id"], "ENTRY", "5")
    r = move(client, product["id"], "EXIT", "6")
    assert r.status_code == 409
    assert balance_of(client, product["id"]) == 5  # nada foi gravado


def test_adjustment_requires_reason(client, product):
    r = move(client, product["id"], "ADJUSTMENT", "-1")
    assert r.status_code == 422


def test_negative_adjustment_cannot_go_below_zero(client, product):
    move(client, product["id"], "ENTRY", "2")
    r = move(client, product["id"], "ADJUSTMENT", "-3", "perda")
    assert r.status_code == 409


def test_balance_matches_history(client, product):
    move(client, product["id"], "ENTRY", "10")
    move(client, product["id"], "EXIT", "3")
    move(client, product["id"], "ADJUSTMENT", "-1", "quebra")
    history = client.get("/api/v1/stock/movements", params={"product_id": product["id"]}).json()
    assert len(history) == 3
    assert balance_of(client, product["id"]) == 6


def test_unknown_product(client):
    assert move(client, 999999, "ENTRY", "1").status_code == 404