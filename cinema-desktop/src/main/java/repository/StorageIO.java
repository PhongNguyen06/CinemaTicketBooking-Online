package repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Compatibility adapter: existing repositories keep their original CSV row formats,
 * but read/write those rows through normalized MySQL tables when enabled.
 * Set CINEMA_STORAGE=mysql, CINEMA_DB_URL, CINEMA_DB_USER, CINEMA_DB_PASSWORD.
 */
public final class StorageIO {
    private StorageIO() {}

    private record Table(String name, String[] columns, int[] numeric) {}
    private static final Map<String, Table> TABLES = new LinkedHashMap<>();
    static {
        add("cinemas", "id,name,address", "0");
        add("movies", "id,title,duration,genre,description", "0,2");
        add("customers", "id,name,email,phone", "0");
        add("rooms", "id,room_name,capacity,cinema_id", "0,2,3");
        add("seats", "id,seat_row,seat_number,seat_type,room_id", "0,2,4");
        add("showtimes", "id,start_time,end_time,status,movie_id,room_id,ticket_price", "0,4,5,6");
        add("users", "id,username,password,role,customer_id", "0,4");
        add("bookings", "id,booking_date,total_amount,status,customer_id", "0,2,4");
        add("tickets", "id,price,status,booking_id,seat_id,showtime_id", "0,1,3,4,5");
    }
    private static void add(String name, String cols, String nums) {
        String[] c = cols.split(",");
        int[] n = Arrays.stream(nums.split(",")).mapToInt(Integer::parseInt).toArray();
        TABLES.put(name, new Table(name,c,n));
    }
    public static boolean mysqlEnabled() {
        return "mysql".equalsIgnoreCase(System.getProperty("cinema.storage",
            System.getenv().getOrDefault("CINEMA_STORAGE", "csv")));
    }
    private static Table table(String file) {
        String name = Paths.get(file).getFileName().toString().replaceFirst("\\.csv$", "");
        Table t = TABLES.get(name);
        if (t == null) throw new IllegalArgumentException("Unknown data file: " + file);
        return t;
    }
    public static Connection connection() throws SQLException {
        String url = System.getProperty("cinema.db.url", System.getenv().getOrDefault(
            "CINEMA_DB_URL", "jdbc:mysql://localhost:3306/cinema_booking?useUnicode=true&characterEncoding=UTF-8"));
        String user = System.getProperty("cinema.db.user", System.getenv().getOrDefault("CINEMA_DB_USER", "cinema_app"));
        String pass = System.getProperty("cinema.db.password", System.getenv().getOrDefault("CINEMA_DB_PASSWORD", ""));
        return DriverManager.getConnection(url, user, pass);
    }
    public static BufferedReader reader(String file) throws IOException {
        if (!mysqlEnabled()) return Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8);
        Table t = table(file);
        String sql = "SELECT " + String.join(",", t.columns) + " FROM " + t.name + " ORDER BY id";
        StringBuilder contents = new StringBuilder();
        try (Connection c = connection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                for (int i=0;i<t.columns.length;i++) {
                    if (i>0) contents.append(',');
                    Object value = rs.getObject(i+1);
                    // Legacy CSV format uses 0 for missing foreign keys.
                    contents.append(value==null && isNumeric(t,i) ? "0" : value==null ? "" : value.toString());
                }
                contents.append('\n');
            }
        } catch (SQLException ex) { throw new IOException("MySQL read failed for " + t.name, ex); }
        return new BufferedReader(new StringReader(contents.toString()));
    }
    public static BufferedWriter writer(String file) throws IOException {
        if (!mysqlEnabled()) {
            Files.createDirectories(Paths.get("database"));
            return Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8);
        }
        StringWriter buffer = new StringWriter();
        return new BufferedWriter(buffer) {
            private boolean saved;
            @Override public void close() throws IOException {
                if (saved) return;
                super.close();
                saved = true;
                List<String> lines = Arrays.asList(buffer.toString().split("\\R"));
                writeRows(table(file), lines, false);
            }
        };
    }
    private static boolean isNumeric(Table t, int index) {
        for (int i:t.numeric) if (i==index) return true;
        return false;
    }
    // Replace one table's entire contents as previous CSV repositories did.
    // Updates and deletions are inside one transaction; FK constraints prevent unsafe deletes.
    private static void writeRows(Table t, List<String> lines, boolean importMode) throws IOException {
        String placeholders = String.join(",", Collections.nCopies(t.columns.length,"?"));
        String updates = String.join(",", Arrays.stream(t.columns).skip(1)
            .map(x -> x+"=VALUES("+x+")").toArray(String[]::new));
        String sql = "INSERT INTO " + t.name + " ("+String.join(",",t.columns)+") VALUES ("+placeholders+") ON DUPLICATE KEY UPDATE "+updates;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                Set<Long> ids=new HashSet<>();
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts=line.split(",",-1);
                    if (parts.length != t.columns.length) throw new IOException(
                        "Invalid "+t.name+" row; expected "+t.columns.length+" columns: "+line);
                    ids.add(Long.parseLong(parts[0]));
                    for (int i=0;i<parts.length;i++) {
                        if (isNumeric(t,i)) {
                            // nullable FK 0 in legacy files
                            if (i!=0 && t.columns[i].endsWith("_id") && (parts[i].isBlank()||parts[i].equals("0")))
                                ps.setNull(i+1,Types.BIGINT);
                            else if (t.columns[i].equals("total_amount")) ps.setBigDecimal(i+1,new java.math.BigDecimal(parts[i]));
                            else ps.setLong(i+1,Long.parseLong(parts[i]));
                        } else ps.setString(i+1,parts[i]);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
                if (!importMode) {
                    // CSV save() semantics replace a whole list; delete removed IDs.
                    try (Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT id FROM "+t.name)) {
                        List<Long> remove=new ArrayList<>();
                        while(rs.next()) if(!ids.contains(rs.getLong(1))) remove.add(rs.getLong(1));
                        try(PreparedStatement del=c.prepareStatement("DELETE FROM "+t.name+" WHERE id=?")) {
                            for(Long id:remove){del.setLong(1,id);del.addBatch();}
                            del.executeBatch();
                        }
                    }
                }
                c.commit();
            } catch(Exception ex) {
                c.rollback();
                throw new IOException("MySQL transaction failed for "+t.name+": "+ex.getMessage(),ex);
            }
        } catch(SQLException ex) { throw new IOException("MySQL connection failed",ex); }
    }
    public static void importAllCsv(Path directory) throws IOException {
        for(Table t:TABLES.values()) {
            Path file=directory.resolve(t.name+".csv");
            if(!Files.isRegularFile(file)) throw new FileNotFoundException(file.toString());
            List<String> rows=Files.readAllLines(file,StandardCharsets.UTF_8);
            writeRows(t,rows,true);
            System.out.println("Imported "+t.name+": "+rows.stream().filter(x->!x.isBlank()).count());
        }
    }
}
