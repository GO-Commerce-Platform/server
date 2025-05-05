package dev.tiodati.saas.gocommerce.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MessageServiceTest {

    @Mock
    @Default
    LocaleResolver localeResolver;
    
    @InjectMocks
    MessageService messageService = new MessageService();
    
    private final Locale ENGLISH = Locale.ENGLISH;
    private final Locale SPANISH = new Locale("es");
    private final Locale PORTUGUESE = new Locale("pt");
    
    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        
        when(localeResolver.getLocale()).thenReturn(ENGLISH);
    }
    
    @Test
    void testGetMessage_withDefaultLocale() {
        assertEquals("Product Name", messageService.getMessage("product.name"));
        assertEquals("First Name", messageService.getMessage("customer.firstName"));
    }
    
    @Test
    void testGetMessage_withSpecificLocale() {
        assertEquals("Nombre del producto", messageService.getMessage("product.name", SPANISH));
        assertEquals("Nome do produto", messageService.getMessage("product.name", PORTUGUESE));
    }
    
    @Test
    void testGetMessage_withParameters() {
        assertEquals("Minimum length is 8 characters", 
                messageService.getMessage("validation.minLength", 8));
    }
    
    @Test
    void testGetMessage_withSpecificLocaleAndParameters() {
        assertEquals("La longitud mínima es de 8 caracteres", 
                messageService.getMessage("validation.minLength", SPANISH, 8));
        assertEquals("O comprimento mínimo é de 8 caracteres", 
                messageService.getMessage("validation.minLength", PORTUGUESE, 8));
    }
    
    @Test
    void testGetMessage_withNonExistentKey() {
        String nonExistentKey = "non.existent.key";
        assertEquals(nonExistentKey, messageService.getMessage(nonExistentKey));
    }
}