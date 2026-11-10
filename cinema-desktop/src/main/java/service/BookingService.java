package service;

import entity.Booking;
import entity.Customer;
import entity.Ticket;
import enums.BookingStatus;
import repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    private final BookingRepository repository;
    private final List<Booking> bookings;

    public BookingService(List<Customer> customers) {
        repository = new BookingRepository();
        bookings = repository.findAll(customers);
    }

    public Booking createBooking(Customer customer) {
        Booking booking = new Booking();

        booking.setId(getNextId());
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalAmount(0);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);

        bookings.add(booking);
        repository.save(bookings);

        return booking;
    }

    public void addBooking(Booking booking) {
        bookings.add(booking);
        repository.save(bookings);
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Booking findById(long id) {
        for (Booking booking : bookings) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }

    public List<Booking> findByCustomer(long customerId) {
        List<Booking> result = new ArrayList<>();

        for (Booking booking : bookings) {
            if (booking.getCustomer() != null
                    && booking.getCustomer().getId() == customerId) {
                result.add(booking);
            }
        }

        return result;
    }

    public double calculateTotal(List<Ticket> tickets) {
        double total = 0;

        for (Ticket ticket : tickets) {
            total += ticket.getPrice();
        }

        return total;
    }

    public void updateTotalAmount(Booking booking, List<Ticket> tickets) {
        if (booking != null) {
            booking.setTotalAmount(calculateTotal(tickets));
            repository.save(bookings);
        }
    }

    public void cancelBooking(long id) {
        Booking booking = findById(id);

        if (booking != null) {
            booking.setStatus(BookingStatus.CANCELLED);
            repository.save(bookings);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Booking booking : bookings) {
            if (booking.getId() > maxId) {
                maxId = booking.getId();
            }
        }

        return maxId + 1;
    }
}
