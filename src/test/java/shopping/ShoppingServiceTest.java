package shopping;

import customer.Customer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import product.Product;
import product.ProductDao;

import java.util.List;
import java.util.Map;

/**
 * Тестирует сервис покупок
 */
@ExtendWith(MockitoExtension.class)
public class ShoppingServiceTest {
    /**
     * Моковый объект взаимодействия с БД для товаров
     */
    private final ProductDao productDaoMock;

    /**
     * Объект тестирования - сервис покупок
     */
    private final ShoppingService shoppingService;

    /**
     * Покупатель
     */
    private final Customer customer;

    public ShoppingServiceTest(@Mock ProductDao productDaoMock) {
        this.productDaoMock = productDaoMock;
        shoppingService = new ShoppingServiceImpl(productDaoMock);
        customer = new Customer(54367L, "+79502546537");
    }

    /**
     * Протестировать получение корзины для покупок.
     * Логическая ошибка: почему объект Cart хранит Customer, должно
     * быть наоборот, покупатель хранит принадлежащую ему корзину
     */
    @Test
    public void testGetCart() {
        Cart cart = shoppingService.getCart(customer);

        Assertions.assertNotNull(cart);
    }

    /**
     * Протестировать получение корзины для покупок с Customer = null.
     * Логическая ошибка: понимаю, что хранить покупателя в корзине уже
     * является ошибкой, но если уж есть такая реализация, почему бы не
     * проверить валидность передаваемого значения. Если Customer = null
     * можно выбрасывать исключение или возвращать объект Cart равный null
     */
    @Test
    public void testGetCartWithInvalidValue() {
        //Проверив конкретный Exception и его сообщение
        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () ->
                shoppingService.getCart(null));
        Assertions.assertEquals("Какое-то сообщение", ex.getMessage());
        //Или так
        Assertions.assertNull(shoppingService.getCart(null));
    }

    /**
     * Протестировать получение всех продуктов
     */
    @Test
    public void testGetAllProducts() {
        Mockito.when(productDaoMock.getAll()).thenReturn(List.of(
                new Product("Масло", 3),
                new Product("Молоко", 2)
        ));

        List<Product> products = shoppingService.getAllProducts();

        Assertions.assertEquals(2, products.size());
        Assertions.assertEquals("Масло", products.get(0).getName());
        Assertions.assertEquals("Молоко", products.get(1).getName());

        Mockito.verify(productDaoMock, Mockito.times(1)).getAll();
    }

    /**
     * Протестировать успешное получение продукта по имени
     */
    @Test
    public void testGetExistentProductByName() {
        Mockito.when(productDaoMock.getByName(Mockito.eq("Хлеб")))
                .thenReturn(new Product("Хлеб", 4));

        Product product = shoppingService.getProductByName("Хлеб");

        Assertions.assertNotNull(product);
        Assertions.assertEquals("Хлеб", product.getName());
        Assertions.assertEquals(4, product.getCount());

        Mockito.verify(productDaoMock, Mockito.times(1))
                .getByName(Mockito.eq("Хлеб"));
    }

    /**
     * Протестировать получение несуществующего продукта по имени
     */
    @Test
    public void testGetNonExistentProductByName() {
        Mockito.when(productDaoMock.getByName(Mockito.eq("Меня нет")))
                .thenReturn(null);

        Product product = shoppingService.getProductByName("Меня нет");

        Assertions.assertNull(product);

        Mockito.verify(productDaoMock, Mockito.times(1))
                .getByName(Mockito.eq("Меня нет"));
    }

    /**
     * Протестировать совершение покупки, когда корзина пуста
     */
    @Test
    public void testBuyWhenCartIsEmpty() throws BuyException {
        Cart cart = shoppingService.getCart(customer);

        Assertions.assertFalse(shoppingService.buy(cart));

        Mockito.verify(productDaoMock, Mockito.never())
                .save(Mockito.any());
    }

    /**
     * Протестировать успешное совершение покупки.
     * Логическая ошибка: при добавлении товара в корзину пишет,
     * что нет необходимого кол-ва. Ошибка в validateCount:
     * product.getCount() - count <= 0,
     * должно быть product.getCount() - count < 0
     */
    @Test
    public void testSuccessfulBuy() throws BuyException {
        Cart cart = shoppingService.getCart(customer);
        Product product = new Product("Хлеб", 3);
        cart.add(product, 3);

        Assertions.assertTrue(shoppingService.buy(cart));

        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(product);
    }

    /**
     * Протестировать совершение покупки при недостаточном кол-ве товара.
     * Чтобы тест не упал на cart.add(), сделал его моковым
     */
    @Test
    public void testBuyThrowsException() {
        Product butter = new Product("Масло", 3);
        Product milk = new Product("Молоко", 2);
        Cart cartMock = Mockito.mock(Cart.class);

        Mockito.when(cartMock.getProducts()).thenReturn(Map.of(
                butter, 4,
                milk, 1
        ));

        BuyException exception = Assertions.assertThrows(BuyException.class, () -> {
            shoppingService.buy(cartMock);
        });
        Assertions.assertEquals("В наличии нет необходимого количества товара 'Масло'",
                exception.getMessage());

        Mockito.verify(productDaoMock, Mockito.never())
                .save(Mockito.any());
    }
}
