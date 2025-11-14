package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ReservationService reservationService;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() {
        // TODO: Implementar el test de creación de reserva exitosa
        ReservationRequestDTO request = new ReservationRequestDTO(
                1L,
                258027L,
                7,
                LocalDate.now()
        );

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        ReservationResponseDTO result = reservationService.createReservation(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testBook.getExternalId(), result.getBookExternalId());
        assertEquals(0, result.getDailyRate().compareTo(testBook.getPrice()));
        verify(bookRepository, times(1)).save(any(Book.class));
        verify(reservationRepository, times(1)).save(any(Reservation.class));

    }
    
    @Test
    void testCreateReservation_BookNotAvailable() {
        // TODO: Implementar el test de creación de reserva cuando el libro no está disponible
        ReservationRequestDTO request = new ReservationRequestDTO(1L, 258027L, 7, LocalDate.now());
        testBook.setAvailableQuantity(0);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));

        assertThrows(RuntimeException.class, () -> reservationService.createReservation(request));
        verify(reservationRepository, times(0)).save(any(Reservation.class));
    }
    
    @Test
    void testReturnBook_OnTime() {
        // TODO: Implementar el test de devolución de libro en tiempo
        ReturnBookRequestDTO request = new ReturnBookRequestDTO(testReservation.getExpectedReturnDate());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(0, result.getLateFee().compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testReturnBook_Overdue() {
        // TODO: Implementar el test de devolución de libro con retraso
        LocalDate returnDate = testReservation.getExpectedReturnDate().plusDays(3);
        ReturnBookRequestDTO request = new ReturnBookRequestDTO(returnDate);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.OVERDUE, result.getStatus());
        // late fee = price * 0.15 * 3 -> 15.99*0.15*3 = 7.1955 -> rounded to 7.20
        assertEquals(0, result.getLateFee().compareTo(new BigDecimal("7.20")));
        // total fee should be original + late fee
        assertEquals(0, result.getTotalFee().compareTo(new BigDecimal("119.13")));

    }
    
    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
    /*
    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
        
        List<ReservationResponseDTO> result = reservationService.getAllReservations();
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    */
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

