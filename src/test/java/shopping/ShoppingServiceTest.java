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

/**
 * Тестирует сервис покупок
 * {@link ShoppingService}
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
     * Протестировать получение пустых корзин для покупок.
     * Логическая ошибка: почему объект Cart хранит Customer, который к тому же нигде
     * не используется, должно быть наоборот, покупатель хранит принадлежащую ему корзину.
     * Логическая ошибка: метод getCart должен возвращать корзину, а не создавать новую.
     */
    @Test
    public void testGetCart() {
        Cart cart1 = shoppingService.getCart(customer);
        Assertions.assertNotNull(cart1);
        Assertions.assertTrue(cart1.getProducts().isEmpty());

        Cart cart2 = shoppingService.getCart(customer);
        Assertions.assertNotNull(cart2);
        Assertions.assertTrue(cart2.getProducts().isEmpty());

        Assertions.assertNotSame(cart1, cart2);
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
        //Тестировать этот метод не нужно, т.к. внутри
        //просто вызывается productDao, который является мокнутым
    }

    /**
     * Протестировать успешное получение продукта по имени
     */
    @Test
    public void testGetExistentProductByName() {
        //Тестировать этот метод не нужно, т.к. внутри
        //просто вызывается productDao, который является мокнутым
    }

    /**
     * Протестировать получение несуществующего продукта по имени
     */
    @Test
    public void testGetNonExistentProductByName() {
        //Тестировать этот метод не нужно, т.к. внутри
        //просто вызывается productDao, который является мокнутым
    }

    /**
     * Протестировать совершение покупки, когда корзина пуста
     */
    @Test
    public void testBuyWhenCartIsEmpty() throws BuyException {
        Cart cart = shoppingService.getCart(customer);

        Assertions.assertTrue(cart.getProducts().isEmpty());
        Assertions.assertFalse(shoppingService.buy(cart));

        Mockito.verify(productDaoMock, Mockito.never())
                .save(Mockito.any());
    }

    /**
     * Протестировать совершение покупки продукта.
     * Логическая ошибка: корзина не очищается после покупки.
     */
    @Test
    public void testBuyProduct() throws BuyException {
        Cart cart = shoppingService.getCart(customer);
        Product product = new Product("Масло", 3);
        cart.add(product, 2);

        Assertions.assertTrue(shoppingService.buy(cart));
        Assertions.assertEquals(1, product.getCount());
        Assertions.assertTrue(cart.getProducts().isEmpty());

        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(product);
    }

    /**
     * Протестировать, что нельзя купить товар с отрицательным значением.
     * Логическая ошибка: можно купить товар с отрицательным значением.
     */
    @Test
    public void testBuyProductWithNegativeValue() throws BuyException {
        Cart cart = shoppingService.getCart(customer);
        Product product = new Product("Масло", 3);
        cart.add(product, -2);

        Assertions.assertFalse(shoppingService.buy(cart));
        Assertions.assertEquals(3, product.getCount());

        Mockito.verify(productDaoMock, Mockito.never())
                .save(Mockito.any());
    }

    /**
     * Протестировать совершение покупки всего оставшегося продукта.
     * Логическая ошибка: при добавлении товара в корзину пишет,
     * что нет необходимого кол-ва. Ошибка в validateCount:
     * product.getCount() - count <= 0,
     * должно быть product.getCount() - count < 0
     */
    @Test
    public void testBuyAllRemainingProduct() throws BuyException {
        Cart cart = shoppingService.getCart(customer);
        Product product = new Product("Хлеб", 3);
        cart.add(product, 3);

        Assertions.assertTrue(shoppingService.buy(cart));
        Assertions.assertEquals(0, product.getCount());
        Assertions.assertTrue(cart.getProducts().isEmpty());

        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(product);
    }

    /**
     * Протестировать совершение покупки при недостаточном кол-ве товара.
     */
    @Test
    public void testBuyThrowsException() throws BuyException {
        Product butter = new Product("Масло", 4);
        Product milk = new Product("Молоко", 2);

        Cart cart1 = shoppingService.getCart(customer);
        cart1.add(butter, 2);

        Cart cart2 = shoppingService.getCart(new Customer(67564L, "8954024365"));
        cart2.add(butter, 3);
        cart2.add(milk, 1);

        shoppingService.buy(cart1);

        BuyException exception = Assertions.assertThrows(BuyException.class, () -> {
            shoppingService.buy(cart2);
        });
        Assertions.assertEquals("В наличии нет необходимого количества товара 'Масло'",
                exception.getMessage());

        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(butter);
    }
}
