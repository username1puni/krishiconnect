package com.krishiconnect.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.BookingKind;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.EquipmentPhoto;
import com.krishiconnect.entity.ModerationStatus;
import com.krishiconnect.entity.NotificationType;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.Quote;
import com.krishiconnect.entity.Requirement;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Review;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.repository.BookingRepository;
import com.krishiconnect.repository.EquipmentPhotoRepository;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.QuoteRepository;
import com.krishiconnect.repository.RequirementRepository;
import com.krishiconnect.repository.ReviewRepository;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.UserRepository;
import com.krishiconnect.service.NotificationService;
import com.krishiconnect.service.SlotWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills an empty database with villages around Karnataka, providers, machines, specialists,
 * open requirements and a year of past bookings, so the demo has something to show.
 * Runs only when SEED_DEMO_DATA=true and there is no equipment yet.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentPhotoRepository photoRepository;
    private final BookingRepository bookingRepository;
    private final RequirementRepository requirementRepository;
    private final QuoteRepository quoteRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final SlotWriter slotWriter;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Value("${app.seed.demo-data}")
    private boolean enabled;

    @Value("${app.seed.demo-password}")
    private String demoPassword;

    private final Random random = new Random(42);
    private final Map<EquipmentCategory, List<byte[]>> photos = new HashMap<>();
    private String passwordHash;

    private record Town(String name, String district, double lat, double lng) {
    }

    private static final Town MANDYA = new Town("Mandya", "Mandya", 12.5223, 76.8970);
    private static final Town KERAGODU = new Town("Keragodu", "Mandya", 12.6146, 76.8580);
    private static final Town MADDUR = new Town("Maddur", "Mandya", 12.5843, 77.0430);
    private static final Town SRIRANGAPATNA = new Town("Srirangapatna", "Mandya", 12.4216, 76.6930);
    private static final Town PANDAVAPURA = new Town("Pandavapura", "Mandya", 12.4985, 76.6743);
    private static final Town MALAVALLI = new Town("Malavalli", "Mandya", 12.3856, 77.0608);
    private static final Town KR_PET = new Town("Krishnarajpet", "Mandya", 12.6615, 76.4870);
    private static final Town BESAGARAHALLI = new Town("Besagarahalli", "Mandya", 12.6386, 76.9560);
    private static final Town MYSURU = new Town("Mysuru", "Mysuru", 12.2958, 76.6394);
    private static final Town BANNUR = new Town("Bannur", "Mysuru", 12.3320, 76.8620);
    private static final Town T_NARASIPURA = new Town("T Narasipura", "Mysuru", 12.2105, 76.9049);
    private static final Town HUNSUR = new Town("Hunsur", "Mysuru", 12.3092, 76.2900);
    private static final Town NANJANGUD = new Town("Nanjangud", "Mysuru", 12.1200, 76.6800);
    private static final Town CHANNAPATNA = new Town("Channapatna", "Ramanagara", 12.6518, 77.2089);
    private static final Town HASSAN = new Town("Hassan", "Hassan", 13.0072, 76.0962);
    private static final Town CHANNARAYAPATNA = new Town("Channarayapatna", "Hassan", 12.9024, 76.3880);
    private static final Town TUMAKURU = new Town("Tumakuru", "Tumakuru", 13.3392, 77.1017);
    private static final Town TIPTUR = new Town("Tiptur", "Tumakuru", 13.2563, 76.4776);
    private static final Town DAVANAGERE = new Town("Davanagere", "Davanagere", 14.4644, 75.9218);
    private static final Town SHIVAMOGGA = new Town("Shivamogga", "Shivamogga", 13.9299, 75.5681);
    private static final Town DHARWAD = new Town("Dharwad", "Dharwad", 15.4589, 75.0078);
    private static final Town RAICHUR = new Town("Raichur", "Raichur", 16.2120, 77.3439);
    private static final Town KALABURAGI = new Town("Kalaburagi", "Kalaburagi", 17.3297, 76.8343);

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || equipmentRepository.count() > 0) {
            return;
        }
        log.info("Seeding demo data");
        passwordHash = passwordEncoder.encode(demoPassword);
        loadPhotos();
        tx.executeWithoutResult(status -> seed());
        log.info("Demo data ready: {} users, {} machines, {} bookings", userRepository.count(),
                equipmentRepository.count(), bookingRepository.count());
    }

    private void seed() {
        // demo logins shown on the login page
        User seeker = user("Ramesh Gowda", "9900000001", "seeker@krishiconnect.demo", Role.SEEKER, KERAGODU, true);
        seeker.setCrops("PADDY,SUGARCANE,RAGI");
        User provider = user("Manjunath Swamy", "9900000002", "provider@krishiconnect.demo", Role.PROVIDER, MANDYA, true);
        User driver = specialist("Basavaraj K", "9900000003", "specialist@krishiconnect.demo", MADDUR, Skill.TRACTOR_DRIVER,
                9, 150, 900, "Kannada, Hindi", "Drive tractors for ploughing, puddling and cane transport. Own licence.", true);

        List<User> seekers = new ArrayList<>(List.of(seeker,
                user("Lakshmi Devi", "9900000011", null, Role.SEEKER, PANDAVAPURA, true),
                user("Siddappa H", "9900000012", null, Role.SEEKER, MALAVALLI, true),
                user("Kavya Nagesh", "9900000013", "kavya.n@example.com", Role.SEEKER, SRIRANGAPATNA, true),
                user("Shivakumar B", "9900000014", null, Role.SEEKER, HUNSUR, true),
                user("Nagaraj Patil", "9900000015", null, Role.SEEKER, DAVANAGERE, true),
                user("Mallikarjun S", "9900000016", null, Role.SEEKER, RAICHUR, true),
                user("Yashodha R", "9900000017", null, Role.SEEKER, BANNUR, true)));

        User p2 = user("Chandrashekar Gowda", "9900000021", null, Role.PROVIDER, BESAGARAHALLI, true);
        User p3 = user("Venkatesh Murthy", "9900000022", null, Role.PROVIDER, SRIRANGAPATNA, true);
        User p4 = user("Prakash Hegde", "9900000023", null, Role.PROVIDER, MYSURU, true);
        User p5 = user("Girish Kumar", "9900000024", null, Role.PROVIDER, T_NARASIPURA, true);
        User p6 = user("Anil Kumar", "9900000025", null, Role.PROVIDER, CHANNARAYAPATNA, true);
        User p7 = user("Harish Reddy", "9900000026", null, Role.PROVIDER, TUMAKURU, true);
        User p8 = user("Mahesh Kori", "9900000027", null, Role.PROVIDER, DAVANAGERE, true);
        User p9 = user("Sunil Desai", "9900000028", null, Role.PROVIDER, DHARWAD, true);
        User p10 = user("Raghavendra Rao", "9900000029", null, Role.PROVIDER, RAICHUR, true);
        User p11 = user("Kiran Shetty", "9900000030", null, Role.PROVIDER, SHIVAMOGGA, true);
        // waiting for admin verification, so its listing is pending
        User newProvider = user("Santosh M", "9900000031", null, Role.PROVIDER, MADDUR, false);

        List<User> specialists = new ArrayList<>(List.of(driver,
                specialist("Shivanna T", "9900000041", null, MANDYA, Skill.TRACTOR_DRIVER, 12, 160, 950, "Kannada", "Tractor and trailer, also rotavator work.", true),
                specialist("Mohan Raj", "9900000042", null, SRIRANGAPATNA, Skill.HARVESTER_OPERATOR, 8, 300, 1400, "Kannada, Tamil", "Operate Kartar and Preet combines, paddy and ragi.", true),
                specialist("Imran Pasha", "9900000043", null, MANDYA, Skill.HARVESTER_OPERATOR, 6, 280, 1300, "Kannada, Hindi, Urdu", "Track type paddy harvester operator.", true),
                specialist("Kavitha S", "9900000044", null, BANNUR, Skill.DRONE_PILOT, 3, 450, 2200, "Kannada, English", "DGCA certified drone pilot. Pesticide and nano urea spraying.", true),
                specialist("Naveen Kumar", "9900000045", null, MYSURU, Skill.DRONE_PILOT, 2, 400, 2000, "Kannada, English, Hindi", "Agri drone spraying for paddy, maize and sugarcane.", true),
                specialist("Puttaswamy", "9900000046", null, PANDAVAPURA, Skill.SPRAY_OPERATOR, 15, 120, 750, "Kannada", "Power sprayer and knapsack spraying.", true),
                specialist("Lokesh G", "9900000047", null, MADDUR, Skill.IRRIGATION_TECHNICIAN, 10, 200, 1100, "Kannada, English", "Drip and sprinkler installation, pump repair.", true),
                specialist("Ravi Shankar", "9900000048", null, MANDYA, Skill.FARM_MECHANIC, 14, 300, 1500, "Kannada, Hindi", "Tractor and power tiller repair, comes to the field.", true),
                specialist("Thimmaiah", "9900000049", null, CHANNAPATNA, Skill.COCONUT_CLIMBER, 20, null, 1100, "Kannada", "Coconut and arecanut harvesting with safety gear.", true),
                specialist("Gangadhar", "9900000050", null, TIPTUR, Skill.COCONUT_CLIMBER, 11, null, 1000, "Kannada", "Coconut harvesting, about 80 trees a day.", true),
                specialist("Manjamma", "9900000051", null, MALAVALLI, Skill.FARM_LABOUR, 18, null, 600, "Kannada", "Team of 12 for transplanting, weeding and cane cutting.", true),
                specialist("Suresh Naik", "9900000052", null, SHIVAMOGGA, Skill.PRUNING_EXPERT, 9, 150, 900, "Kannada, Konkani", "Arecanut and mango pruning, Bordeaux spraying.", true),
                specialist("Basappa Hosamani", "9900000053", null, DHARWAD, Skill.HARVESTER_OPERATOR, 7, 300, 1400, "Kannada, Hindi", "Combine operator for jowar, chickpea and wheat.", true),
                specialist("Arun Kumar", "9900000054", null, KR_PET, Skill.TRACTOR_DRIVER, 4, 140, 850, "Kannada", "New to the platform, five years of driving.", false),
                specialist("Deepa M", "9900000055", null, HASSAN, Skill.DRONE_PILOT, 1, 400, 1900, "Kannada, English", "Recently trained drone pilot.", false)));

        List<Equipment> machines = new ArrayList<>();
        machines.add(machine(provider, "Mahindra 575 DI tractor with rotavator", EquipmentCategory.ROTAVATOR, "Mahindra", "575 DI XP Plus", 2021, 47, "1100", null, "1500", true, MANDYA, "7 ft Shaktiman rotavator. Good for puddling paddy fields and breaking cane stubble. Driver included."));
        machines.add(machine(provider, "Swaraj 744 FE tractor", EquipmentCategory.TRACTOR, "Swaraj", "744 FE", 2019, 48, "900", "6500", "1200", true, MANDYA, "Ploughing, cultivator and trailer work. Comes with an experienced driver."));
        machines.add(machine(provider, "Kubota NSP-4W paddy transplanter", EquipmentCategory.PADDY_TRANSPLANTER, "Kubota", "NSP-4W", 2022, null, null, "9000", "3400", true, MANDYA, "4 row walk behind transplanter. Mat nursery must be ready. Operator included."));
        machines.add(machine(p2, "John Deere 5050D with cultivator", EquipmentCategory.CULTIVATOR, "John Deere", "5050D", 2020, 50, "950", null, "1150", true, BESAGARAHALLI, "9 tine spring cultivator. Ideal before ragi and maize sowing."));
        machines.add(machine(p2, "Kartar 4000 combine harvester", EquipmentCategory.COMBINE_HARVESTER, "Kartar", "4000", 2020, 101, "2600", null, "2400", true, BESAGARAHALLI, "Wheel type combine for paddy and ragi. Harvests about 1 acre an hour in dry fields."));
        machines.add(machine(p2, "Sonalika DI 745 III tractor", EquipmentCategory.TRACTOR, "Sonalika", "DI 745 III", 2018, 50, "850", "6000", "1100", true, BESAGARAHALLI, "With trailer for cane transport to the factory."));
        machines.add(machine(p3, "Preet 987 track combine harvester", EquipmentCategory.COMBINE_HARVESTER, "Preet", "987 Track", 2021, 101, "2800", null, "2600", true, SRIRANGAPATNA, "Track type, works in wet paddy fields where wheel harvesters get stuck."));
        machines.add(machine(p3, "VST Shakti 130 DI power tiller", EquipmentCategory.POWER_TILLER, "VST", "Shakti 130 DI", 2022, 13, "400", "2600", null, false, SRIRANGAPATNA, "Small fields, coconut and arecanut gardens. Fuel extra."));
        machines.add(machine(p3, "Garuda Kisan drone (10 L)", EquipmentCategory.AGRI_DRONE, "Garuda Aerospace", "Kisan Drone", 2023, null, null, null, "550", true, SRIRANGAPATNA, "10 litre tank, sprays one acre in about 8 minutes. Licensed pilot included."));
        machines.add(machine(p4, "Eicher 380 tractor", EquipmentCategory.TRACTOR, "Eicher", "380", 2017, 40, "800", "5500", "1000", true, MYSURU, "Light tractor, good for small plots and orchards."));
        machines.add(machine(p4, "Mahindra seed cum fertilizer drill (9 row)", EquipmentCategory.SEED_DRILL, "Mahindra", "9 row drill", 2021, null, "900", null, "1000", true, MYSURU, "Seed and fertilizer in one pass. Ragi, maize, groundnut and pulses."));
        machines.add(machine(p4, "Aspee HTP power sprayer", EquipmentCategory.SPRAYER, "Aspee", "HTP 3 piston", 2022, null, "90", "550", null, false, MYSURU, "Engine sprayer with 50 m hose. You need your own operator or book one here."));
        machines.add(machine(p5, "New Holland 3630 TX with rotavator", EquipmentCategory.ROTAVATOR, "New Holland", "3630 TX", 2022, 55, "1200", null, "1650", true, T_NARASIPURA, "Heavy duty rotavator, sugarcane fields."));
        machines.add(machine(p5, "Dasmesh multi crop thresher", EquipmentCategory.THRESHER, "Dasmesh", "Multi crop", 2019, null, "800", "5200", null, true, T_NARASIPURA, "Ragi, jowar, tur and maize. Tractor and operator included."));
        machines.add(machine(p5, "Kirloskar 5 HP diesel pump set", EquipmentCategory.WATER_PUMP, "Kirloskar", "KDS 5 HP", 2020, 5, "120", "700", null, false, T_NARASIPURA, "3 inch outlet, 30 m pipe included."));
        machines.add(machine(p6, "Swaraj 855 FE tractor", EquipmentCategory.TRACTOR, "Swaraj", "855 FE", 2021, 52, "950", "7000", "1250", true, CHANNARAYAPATNA, "Ploughing and trailer work around Channarayapatna."));
        machines.add(machine(p6, "Shaktiman round baler", EquipmentCategory.BALER, "Shaktiman", "Round baler", 2022, null, "1300", null, "1800", true, CHANNARAYAPATNA, "Paddy and ragi straw baling. Bales are about 25 kg each."));
        machines.add(machine(p7, "Mahindra 265 DI tractor", EquipmentCategory.TRACTOR, "Mahindra", "265 DI", 2016, 30, "700", "5000", "950", true, TUMAKURU, "Light tractor for groundnut and coconut farms."));
        machines.add(machine(p7, "IoTechWorld Agribot drone", EquipmentCategory.AGRI_DRONE, "IoTechWorld", "Agribot", 2023, null, null, null, "500", true, TUMAKURU, "Spraying for groundnut and coconut. Pilot included."));
        machines.add(machine(p8, "Claas Crop Tiger 30 combine", EquipmentCategory.COMBINE_HARVESTER, "Claas", "Crop Tiger 30", 2019, 76, "2400", null, "2200", true, DAVANAGERE, "Paddy and maize harvesting around Davanagere and Harihar."));
        machines.add(machine(p8, "Sonalika Tiger DI 50 with seed drill", EquipmentCategory.SEED_DRILL, "Sonalika", "Tiger DI 50", 2021, 50, "950", null, "1050", true, DAVANAGERE, "11 row drill for maize sowing."));
        machines.add(machine(p8, "Maize sheller", EquipmentCategory.THRESHER, "Amar", "Maize sheller", 2020, null, "700", "4800", null, true, DAVANAGERE, "Tractor PTO driven maize sheller."));
        machines.add(machine(p9, "John Deere 5310 tractor", EquipmentCategory.TRACTOR, "John Deere", "5310", 2022, 55, "1000", "7500", "1300", true, DHARWAD, "Black soil ploughing, cotton and jowar fields."));
        machines.add(machine(p9, "Tractor mounted boom sprayer", EquipmentCategory.SPRAYER, "Fieldking", "Boom sprayer 400 L", 2021, null, null, null, "380", true, DHARWAD, "12 m boom, cotton and soybean."));
        machines.add(machine(p10, "Kartar 4000 combine (Raichur)", EquipmentCategory.COMBINE_HARVESTER, "Kartar", "4000", 2018, 101, "2400", null, "2200", true, RAICHUR, "Paddy harvesting in the Tungabhadra canal area."));
        machines.add(machine(p10, "Mahindra Arjun 605 tractor", EquipmentCategory.TRACTOR, "Mahindra", "Arjun 605", 2020, 57, "950", "7000", "1250", true, RAICHUR, "Puddling and transport."));
        machines.add(machine(p11, "Honda 4 stroke power sprayer", EquipmentCategory.SPRAYER, "Honda", "GX35 sprayer", 2023, null, "80", "500", null, false, SHIVAMOGGA, "Arecanut Bordeaux spraying, comes with 30 m hose."));
        machines.add(machine(p11, "Kubota MU4501 tractor", EquipmentCategory.TRACTOR, "Kubota", "MU4501", 2021, 45, "900", "6500", "1150", true, SHIVAMOGGA, "Paddy and arecanut garden work."));
        machines.add(machine(p6, "Fieldking tine cultivator with tractor", EquipmentCategory.CULTIVATOR, "Fieldking", "11 tine", 2019, 45, "850", null, "1050", true, HASSAN, "Potato and maize land preparation."));
        machines.add(machine(p7, "Kirloskar 7.5 HP electric pump", EquipmentCategory.WATER_PUMP, "Kirloskar", "7.5 HP", 2021, 7, "100", "600", null, false, TIPTUR, "Needs a 3 phase connection at the field."));
        machines.add(machine(p4, "Dhaincha straw baler", EquipmentCategory.BALER, "Dashmesh", "Square baler", 2020, null, "1200", null, "1700", true, NANJANGUD, "Square bales, easy to stack."));
        machines.add(machine(p10, "Paddy thresher (Raichur)", EquipmentCategory.THRESHER, "Kartar", "Paddy thresher", 2018, null, "750", "5000", null, true, KALABURAGI, "Also used for tur and jowar."));
        machines.add(machine(newProvider, "Mahindra Yuvo 415 DI tractor", EquipmentCategory.TRACTOR, "Mahindra", "Yuvo 415 DI", 2023, 42, "900", "6500", "1150", true, MADDUR, "New tractor, available from this season."));
        machines.get(machines.size() - 1).setModerationStatus(ModerationStatus.PENDING);
        equipmentRepository.saveAll(machines);
        for (Equipment e : machines) {
            addPhotos(e);
        }

        LocalDateTime now = LocalDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
        history(seekers, machines, specialists, now);
        current(seeker, provider, driver, seekers, machines, now);
        requirements(seeker, seekers, provider, p2, p3, p5, specialists, machines, now);
    }

    /** A year of finished bookings, heavier in each machine's season, so the admin chart looks real. */
    private void history(List<User> seekers, List<Equipment> machines, List<User> specialists, LocalDateTime now) {
        String[] comments = {
                "Came on time and finished the work properly.",
                "Good machine, three acres done before noon.",
                "Fair price. Will book again for the next harvest.",
                "Driver was a little late but the work was clean.",
                "Very experienced, knows paddy fields well.",
                "Machine was well maintained, no breakdown.",
                "Polite and careful around the bunds.",
                "Worked till evening to finish everything. Thank you.",
                null, null};
        List<Equipment> live = machines.stream().filter(Equipment::isBookable).toList();
        List<User> liveSpecialists = specialists.stream().filter(User::isVerified).toList();
        List<Booking> bookings = new ArrayList<>();
        List<Review> reviews = new ArrayList<>();
        for (int monthsAgo = 11; monthsAgo >= 0; monthsAgo--) {
            LocalDate monthStart = now.toLocalDate().minusMonths(monthsAgo).withDayOfMonth(1);
            int month = monthStart.getMonthValue();
            for (Equipment e : live) {
                int count = Math.max(0, (int) Math.round(seasonWeight(e.getCategory(), month) * 2.2 + random.nextGaussian() * 0.6));
                for (int k = 0; k < count; k++) {
                    LocalDateTime start = monthStart.atTime(6 + random.nextInt(4), 0).plusDays(random.nextInt(27));
                    if (!start.plusHours(8).isBefore(now.minusDays(1))) {
                        continue;
                    }
                    int hours = 3 + random.nextInt(6);
                    User farmer = seekers.get(random.nextInt(seekers.size()));
                    Booking b = booking(farmer, start, start.plusHours(hours), BookingStatus.COMPLETED);
                    BookingItem item = equipmentItem(e, start, start.plusHours(hours), BookingItemStatus.COMPLETED);
                    b.addItem(item);
                    // now and then a driver was hired along with the machine
                    if (e.getCategory() == EquipmentCategory.TRACTOR && random.nextInt(4) == 0) {
                        User d = liveSpecialists.get(random.nextInt(2));
                        b.addItem(specialistItem(d, start, start.plusHours(hours), BookingItemStatus.COMPLETED));
                        b.setKind(BookingKind.BUNDLE);
                    }
                    bookings.add(b);
                }
            }
            for (User s : liveSpecialists) {
                int count = random.nextInt(3);
                for (int k = 0; k < count; k++) {
                    LocalDateTime start = monthStart.atTime(7, 0).plusDays(random.nextInt(27));
                    if (!start.plusHours(9).isBefore(now.minusDays(1))) {
                        continue;
                    }
                    Booking b = booking(seekers.get(random.nextInt(seekers.size())), start, start.plusHours(9),
                            BookingStatus.COMPLETED);
                    b.addItem(specialistItem(s, start, start.plusHours(9), BookingItemStatus.COMPLETED));
                    bookings.add(b);
                }
            }
        }
        bookingRepository.saveAll(bookings);
        Map<Long, int[]> equipmentRatings = new HashMap<>();
        Map<Long, int[]> specialistRatings = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getSeeker().getPhone().equals("9900000001") && b.getStartAt().isAfter(now.minusDays(20))) {
                continue; // keep a recent one unreviewed for the demo login
            }
            for (BookingItem i : b.getItems()) {
                if (random.nextInt(10) < 6) {
                    int rating = random.nextInt(10) < 7 ? 5 : 3 + random.nextInt(2);
                    Review r = new Review();
                    r.setBookingItemId(i.getId());
                    r.setReviewer(b.getSeeker());
                    r.setResourceType(i.getResourceType());
                    r.setRating(rating);
                    r.setComment(comments[random.nextInt(comments.length)]);
                    r.setCreatedAt(b.getEndAt().plusHours(5).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
                    if (i.getResourceType() == ResourceType.EQUIPMENT) {
                        r.setEquipmentId(i.getEquipment().getId());
                        equipmentRatings.computeIfAbsent(r.getEquipmentId(), x -> new int[2]);
                        equipmentRatings.get(r.getEquipmentId())[0] += rating;
                        equipmentRatings.get(r.getEquipmentId())[1]++;
                    } else {
                        r.setSpecialistId(i.getSpecialist().getId());
                        specialistRatings.computeIfAbsent(r.getSpecialistId(), x -> new int[2]);
                        specialistRatings.get(r.getSpecialistId())[0] += rating;
                        specialistRatings.get(r.getSpecialistId())[1]++;
                    }
                    reviews.add(r);
                }
            }
        }
        reviewRepository.saveAll(reviews);
        for (Equipment e : machines) {
            int[] sum = equipmentRatings.get(e.getId());
            if (sum != null) {
                e.setRatingSum(sum[0]);
                e.setRatingCount(sum[1]);
            }
        }
        for (SpecialistProfile p : specialistRepository.findAll()) {
            int[] sum = specialistRatings.get(p.getUserId());
            if (sum != null) {
                p.setRatingSum(sum[0]);
                p.setRatingCount(sum[1]);
            }
        }
    }

    /** Upcoming and waiting bookings for the demo logins, so every screen has something on it. */
    private void current(User seeker, User provider, User driver, List<User> seekers, List<Equipment> machines,
                         LocalDateTime now) {
        Equipment rotavator = machines.get(0);
        Equipment swaraj = machines.get(1);
        Equipment combine = machines.get(4);
        LocalDateTime tomorrow6 = now.toLocalDate().plusDays(1).atTime(6, 0);

        // confirmed: the demo farmer booked the Swaraj with a driver for the day after tomorrow
        Booking confirmed = booking(seeker, tomorrow6.plusDays(1), tomorrow6.plusDays(1).plusHours(8), BookingStatus.CONFIRMED);
        confirmed.addItem(equipmentItem(swaraj, confirmed.getStartAt(), confirmed.getEndAt(), BookingItemStatus.ACCEPTED));
        confirmed.addItem(specialistItem(driver, confirmed.getStartAt(), confirmed.getEndAt(), BookingItemStatus.ACCEPTED));
        confirmed.setKind(BookingKind.BUNDLE);
        confirmed.setConfirmedAt(clock.instant().minusSeconds(7200));

        // two requests waiting for the demo provider to answer
        Booking pending1 = booking(seekers.get(1), tomorrow6.plusDays(3), tomorrow6.plusDays(3).plusHours(6), BookingStatus.PENDING);
        pending1.addItem(equipmentItem(rotavator, pending1.getStartAt(), pending1.getEndAt(), BookingItemStatus.PENDING));
        pending1.setExpiresAt(clock.instant().plusSeconds(3600L * 20));
        Booking pending2 = booking(seekers.get(2), tomorrow6.plusDays(5), tomorrow6.plusDays(5).plusHours(4), BookingStatus.PENDING);
        pending2.addItem(equipmentItem(swaraj, pending2.getStartAt(), pending2.getEndAt(), BookingItemStatus.PENDING));
        pending2.setExpiresAt(clock.instant().plusSeconds(3600L * 20));

        // someone else already has the combine next week (shows up on its calendar)
        Booking other = booking(seekers.get(3), tomorrow6.plusDays(6), tomorrow6.plusDays(7).plusHours(12), BookingStatus.CONFIRMED);
        other.addItem(equipmentItem(combine, other.getStartAt(), other.getEndAt(), BookingItemStatus.ACCEPTED));
        other.setConfirmedAt(clock.instant());

        // finished last week and not reviewed yet: the demo farmer can rate it
        LocalDateTime lastWeek = now.toLocalDate().minusDays(5).atTime(7, 0);
        Booking done = booking(seeker, lastWeek, lastWeek.plusHours(5), BookingStatus.COMPLETED);
        done.addItem(equipmentItem(rotavator, lastWeek, lastWeek.plusHours(5), BookingItemStatus.COMPLETED));

        bookingRepository.saveAll(List.of(confirmed, pending1, pending2, other, done));
        for (Booking b : List.of(confirmed, pending1, pending2, other)) {
            b.getItems().forEach(slotWriter::reserve);
        }

        notificationService.notify(seeker.getId(), NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
                "Your booking #" + confirmed.getId() + " for Swaraj 744 FE tractor + Basavaraj K is confirmed.",
                "/bookings", Map.of("bookingId", confirmed.getId(), "resource", "Swaraj 744 FE tractor + Basavaraj K"));
        notificationService.notify(seeker.getId(), NotificationType.BOOKING_COMPLETED, "Work completed",
                "Booking #" + done.getId() + " is marked as done. Please rate " + rotavator.getTitle() + ".",
                "/bookings", Map.of("bookingId", done.getId(), "resource", rotavator.getTitle()));
        for (Booking b : List.of(pending1, pending2)) {
            notificationService.notify(provider.getId(), NotificationType.BOOKING_REQUESTED, "New booking request",
                    b.getSeeker().getFullName() + " wants " + b.getItems().get(0).resourceName() + ". Please accept or reject.",
                    "/provider/bookings", Map.of("bookingId", b.getId(), "name", b.getSeeker().getFullName(),
                            "resource", b.getItems().get(0).resourceName()));
        }
    }

    private void requirements(User seeker, List<User> seekers, User provider, User p2, User p3, User p5,
                              List<User> specialists, List<Equipment> machines, LocalDateTime now) {
        LocalDateTime d = now.toLocalDate().plusDays(4).atTime(7, 0);
        Requirement harvest = requirement(seeker, "Need a combine harvester for 4 acres of paddy", ResourceType.EQUIPMENT,
                EquipmentCategory.COMBINE_HARVESTER, null, "PADDY", "4", d, d.plusHours(5), KERAGODU, "9500",
                "Field is dry, harvester can enter from the main road side. Straw to be left in the field.");
        Requirement driverNeed = requirement(seeker, "Tractor driver for sugarcane transport, 3 days", ResourceType.SPECIALIST,
                null, Skill.TRACTOR_DRIVER, "SUGARCANE", null, d.plusDays(3), d.plusDays(5).plusHours(10), KERAGODU, "3000",
                "Our own tractor and trailer. Need a driver to take cane to the Mandya sugar factory.");
        requirement(seekers.get(1), "Drone spraying for 6 acres paddy (stem borer)", ResourceType.EQUIPMENT,
                EquipmentCategory.AGRI_DRONE, null, "PADDY", "6", d.plusDays(1), d.plusDays(1).plusHours(3), PANDAVAPURA, "3600",
                null);
        requirement(seekers.get(2), "Rotavator for 3 acres before cane planting", ResourceType.EQUIPMENT,
                EquipmentCategory.ROTAVATOR, null, "SUGARCANE", "3", d.plusDays(2), d.plusDays(2).plusHours(5), MALAVALLI, "5000",
                "Black soil, field is ploughed once already.");
        requirement(seekers.get(3), "Labour team for paddy transplanting", ResourceType.SPECIALIST, null, Skill.FARM_LABOUR,
                "PADDY", "2", d.plusDays(6), d.plusDays(6).plusHours(9), SRIRANGAPATNA, "7000", "Around 10 people needed for one day.");
        requirement(seekers.get(4), "Ragi thresher for 2 days", ResourceType.EQUIPMENT, EquipmentCategory.THRESHER, null,
                "RAGI", null, d.plusDays(8), d.plusDays(9).plusHours(10), HUNSUR, null, null);
        requirement(seekers.get(5), "Maize sowing with seed drill, 8 acres", ResourceType.EQUIPMENT,
                EquipmentCategory.SEED_DRILL, null, "MAIZE", "8", d.plusDays(2), d.plusDays(2).plusHours(8), DAVANAGERE, "8500", null);
        requirement(seekers.get(7), "Coconut harvest, about 120 trees", ResourceType.SPECIALIST, null, Skill.COCONUT_CLIMBER,
                "COCONUT", null, d.plusDays(3), d.plusDays(4).plusHours(10), BANNUR, "2500", null);

        // two providers have already quoted on the demo farmer's harvest post
        quote(harvest, p2, machines.get(4), "9000", "Can come early morning. Operator and diesel included.");
        quote(harvest, p3, machines.get(6), "10500", "Track harvester, no problem even if the field is a bit wet.");
        quote(driverNeed, specialists.get(1), null, "2700", "Have driven cane trailers to the factory for 10 years.");
        notificationService.notify(seeker.getId(), NotificationType.QUOTE_RECEIVED, "New quote",
                p2.getFullName() + " quoted Rs 9000 for \"" + harvest.getTitle() + "\".", "/requirements/" + harvest.getId(),
                Map.of("name", p2.getFullName(), "amount", 9000, "title", harvest.getTitle(), "requirementId", harvest.getId()));
    }

    private User user(String name, String phone, String email, Role role, Town town, boolean verified) {
        User u = new User(name, phone, email, passwordHash, role);
        u.setVillage(town.name());
        u.setDistrict(town.district());
        u.setLatitude(jitter(town.lat()));
        u.setLongitude(jitter(town.lng()));
        u.setVerified(verified);
        u.setPreferredLang(random.nextInt(3) == 0 ? "kn" : "en");
        u.setCreatedAt(clock.instant().minusSeconds(86400L * (30 + random.nextInt(300))));
        return userRepository.save(u);
    }

    private User specialist(String name, String phone, String email, Town town, Skill skill, int years, Integer hourly,
                            int daily, String languages, String bio, boolean verified) {
        User u = user(name, phone, email, Role.SPECIALIST, town, verified);
        SpecialistProfile p = new SpecialistProfile(u, skill);
        p.setExperienceYears(years);
        p.setRatePerHour(hourly == null ? null : BigDecimal.valueOf(hourly));
        p.setRatePerDay(BigDecimal.valueOf(daily));
        p.setLanguages(languages);
        p.setBio(bio);
        specialistRepository.save(p);
        return u;
    }

    private Equipment machine(User owner, String title, EquipmentCategory category, String brand, String model, Integer year,
                              Integer hp, String perHour, String perDay, String perAcre, boolean withOperator, Town town,
                              String description) {
        Equipment e = new Equipment();
        e.setOwner(owner);
        e.setTitle(title);
        e.setCategory(category);
        e.setBrand(brand);
        e.setModel(model);
        e.setYearOfMake(year);
        e.setHorsePower(hp);
        e.setPricePerHour(perHour == null ? null : new BigDecimal(perHour));
        e.setPricePerDay(perDay == null ? null : new BigDecimal(perDay));
        e.setPricePerAcre(perAcre == null ? null : new BigDecimal(perAcre));
        e.setWithOperator(withOperator);
        e.setVillage(town.name());
        e.setDistrict(town.district());
        e.setLatitude(jitter(town.lat()));
        e.setLongitude(jitter(town.lng()));
        e.setDescription(description);
        e.setModerationStatus(ModerationStatus.APPROVED);
        e.setCreatedAt(clock.instant().minusSeconds(86400L * (20 + random.nextInt(200))));
        return e;
    }

    private Booking booking(User seeker, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking b = new Booking();
        b.setSeeker(seeker);
        b.setKind(BookingKind.SINGLE);
        b.setStatus(status);
        b.setStartAt(start);
        b.setEndAt(end);
        b.setCreatedAt(start.minusDays(2 + random.nextInt(5)).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        if (status == BookingStatus.COMPLETED) {
            b.setConfirmedAt(b.getCreatedAt().plusSeconds(3600));
            b.setClosedAt(end.atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        }
        return b;
    }

    private BookingItem equipmentItem(Equipment e, LocalDateTime start, LocalDateTime end, BookingItemStatus status) {
        BookingItem i = new BookingItem();
        i.setResourceType(ResourceType.EQUIPMENT);
        i.setEquipment(e);
        i.setProvider(e.getOwner());
        setPrice(i, start, end, e.getPricePerHour(), e.getPricePerAcre(), e.getPricePerDay());
        i.setStatus(status);
        return i;
    }

    private BookingItem specialistItem(User s, LocalDateTime start, LocalDateTime end, BookingItemStatus status) {
        SpecialistProfile p = specialistRepository.findById(s.getId()).orElseThrow();
        BookingItem i = new BookingItem();
        i.setResourceType(ResourceType.SPECIALIST);
        i.setSpecialist(s);
        i.setProvider(s);
        setPrice(i, start, end, p.getRatePerHour(), null, p.getRatePerDay());
        i.setStatus(status);
        return i;
    }

    private void setPrice(BookingItem i, LocalDateTime start, LocalDateTime end, BigDecimal perHour, BigDecimal perAcre,
                          BigDecimal perDay) {
        i.setStartAt(start);
        i.setEndAt(end);
        long hours = Duration.between(start, end).toHours();
        if (perAcre != null && (random.nextBoolean() || (perHour == null && perDay == null))) {
            BigDecimal acres = BigDecimal.valueOf(1 + random.nextInt(5));
            i.setPriceUnit(PriceUnit.ACRE);
            i.setQuantity(acres);
            i.setUnitPrice(perAcre);
        } else if (perHour != null) {
            i.setPriceUnit(PriceUnit.HOUR);
            i.setQuantity(BigDecimal.valueOf(hours));
            i.setUnitPrice(perHour);
        } else {
            i.setPriceUnit(PriceUnit.DAY);
            i.setQuantity(BigDecimal.ONE);
            i.setUnitPrice(perDay);
        }
        i.setAmount(i.getQuantity().multiply(i.getUnitPrice()));
    }

    private Requirement requirement(User seeker, String title, ResourceType type, EquipmentCategory category, Skill skill,
                                    String crop, String acres, LocalDateTime start, LocalDateTime end, Town town,
                                    String budget, String description) {
        Requirement r = new Requirement();
        r.setSeeker(seeker);
        r.setTitle(title);
        r.setNeedType(type);
        r.setCategory(category);
        r.setSkill(skill);
        r.setCrop(crop);
        r.setAreaAcres(acres == null ? null : new BigDecimal(acres));
        r.setStartAt(start);
        r.setEndAt(end);
        r.setVillage(town.name());
        r.setDistrict(town.district());
        r.setLatitude(jitter(town.lat()));
        r.setLongitude(jitter(town.lng()));
        r.setBudget(budget == null ? null : new BigDecimal(budget));
        r.setDescription(description);
        r.setCreatedAt(clock.instant().minusSeconds(3600L * (2 + random.nextInt(40))));
        return requirementRepository.save(r);
    }

    private void quote(Requirement r, User provider, Equipment equipment, String amount, String message) {
        Quote q = new Quote();
        q.setRequirement(r);
        q.setProvider(provider);
        q.setEquipment(equipment);
        q.setAmount(new BigDecimal(amount));
        q.setMessage(message);
        quoteRepository.save(q);
    }

    private void addPhotos(Equipment e) {
        List<byte[]> list = photos.getOrDefault(e.getCategory(), List.of());
        if (list.isEmpty()) {
            return;
        }
        // rotate through the category's photos so neighbours don't all look the same
        int first = (int) (e.getId() % list.size());
        photoRepository.save(new EquipmentPhoto(e, "image/jpeg", list.get(first), 0));
        if (list.size() > 1) {
            photoRepository.save(new EquipmentPhoto(e, "image/jpeg", list.get((first + 1) % list.size()), 1));
        }
    }

    private void loadPhotos() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:seed/photos/*.jpg");
            for (Resource file : files) {
                String name = file.getFilename();
                String category = name.substring(0, name.lastIndexOf('-')).toUpperCase();
                try (InputStream in = file.getInputStream()) {
                    photos.computeIfAbsent(EquipmentCategory.valueOf(category), c -> new ArrayList<>()).add(in.readAllBytes());
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not load demo photos: {}", e.getMessage());
        }
    }

    private double jitter(double value) {
        return Math.round((value + (random.nextDouble() - 0.5) * 0.04) * 1_000_000d) / 1_000_000d;
    }

    /** Rough demand by month (1 = January) for each kind of machine in Karnataka. */
    static double seasonWeight(EquipmentCategory category, int month) {
        return switch (category) {
            case TRACTOR -> in(month, 5, 7) ? 1.0 : in(month, 12, 2) ? 0.8 : 0.4;
            case ROTAVATOR, CULTIVATOR -> in(month, 5, 7) ? 1.0 : in(month, 10, 1) ? 0.6 : 0.2;
            case POWER_TILLER -> in(month, 6, 8) ? 0.9 : 0.3;
            case SEED_DRILL -> in(month, 6, 7) || in(month, 10, 11) ? 1.0 : 0.1;
            case PADDY_TRANSPLANTER -> in(month, 7, 8) ? 1.0 : month == 1 ? 0.6 : 0.0;
            case SPRAYER, AGRI_DRONE -> in(month, 8, 10) ? 1.0 : in(month, 6, 7) ? 0.6 : 0.2;
            case COMBINE_HARVESTER -> in(month, 11, 12) ? 1.2 : in(month, 4, 5) ? 0.8 : in(month, 2, 3) ? 0.4 : 0.1;
            case THRESHER -> in(month, 11, 1) ? 1.0 : in(month, 2, 3) || in(month, 9, 10) ? 0.6 : 0.1;
            case BALER -> in(month, 11, 1) ? 1.0 : in(month, 4, 5) ? 0.5 : 0.0;
            case WATER_PUMP -> in(month, 3, 5) ? 1.0 : 0.2;
        };
    }

    private static boolean in(int month, int from, int to) {
        return from <= to ? month >= from && month <= to : month >= from || month <= to;
    }
}
