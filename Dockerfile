# Sử dụng JDK nhẹ để chạy ứng dụng
FROM eclipse-temurin:17-jre-alpine

# Khai báo thư mục làm việc trong container
WORKDIR /app

# Copy file .jar được build từ Gradle vào container
# Lưu ý: Tên file .jar mặc định thường là [tên-dự-án]-[phiên-bản].jar
COPY build/libs/*.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]