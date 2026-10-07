package com.capstone.jobtracker.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DDay {
    // 표시 라벨: null → "--", 미래 → D-n, 오늘 → D-Day, 지남 → Overdue n
    public static String label(LocalDate dueDate) {
        if (dueDate == null) return "--";
        long d = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (d > 0) return "D-" + d;
        if (d == 0) return "D-Day";
        return "Overdue " + Math.abs(d);
    }

    // 지남 여부(스타일 분기에 사용 가능)
    public static boolean overdue(LocalDate dueDate) {
        if (dueDate == null) return false;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate) < 0;
    }
}
