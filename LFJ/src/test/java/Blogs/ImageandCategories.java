package Blogs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ImageandCategories {
    public static void main(String[] args) throws IOException {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        
        String excelPath = ".//Excel//ImageandCategories.xlsx";
        String reportPath = ".//Excel//ImageandCategoriesReport.xlsx";
        FileInputStream fis = new FileInputStream(new File(excelPath));
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        Workbook reportWorkbook = new XSSFWorkbook();
        Sheet reportSheet = reportWorkbook.createSheet("Report");
        Row headerRow = reportSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Live URL");
        headerRow.createCell(1).setCellValue("Dev URL");
        headerRow.createCell(2).setCellValue("Expected Image");
        headerRow.createCell(3).setCellValue("Actual Image Found");
        headerRow.createCell(4).setCellValue("Status");
        headerRow.createCell(5).setCellValue("Expected Category");
        headerRow.createCell(6).setCellValue("Actual Category Found");
        headerRow.createCell(7).setCellValue("Status");
        
        System.out.println("Starting verification process...");
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Cell liveUrlCell = row.getCell(0);
            Cell devUrlCell = row.getCell(1);
            
            if (liveUrlCell == null || devUrlCell == null || liveUrlCell.getStringCellValue().trim().isEmpty() || devUrlCell.getStringCellValue().trim().isEmpty()) {
                continue;
            }
            
            String liveUrl = liveUrlCell.getStringCellValue();
            String devUrl = devUrlCell.getStringCellValue();
            
            System.out.println("Processing: " + devUrl);
            driver.get(devUrl);
            
            // Verify Image
            boolean imageFound = false;
            String actualImage = "Not Found";
            Cell imageNameCell = row.getCell(2);
            String expectedImage = (imageNameCell != null) ? imageNameCell.getStringCellValue().trim().replaceAll("[^a-zA-Z0-9]", "") : "";
            if (!expectedImage.isEmpty()) {
                System.out.println("Verifying image: " + expectedImage);
                List<WebElement> images = driver.findElements(By.xpath("//*[@class='col-lg-8 col-xl-7']/*/img"));
                for (WebElement img : images) {
                    String src = img.getAttribute("src").replaceAll("[^a-zA-Z0-9]", "");
                    if (src.contains(expectedImage)) {
                        imageFound = true;
                        actualImage = src;
                        break;
                    }
                }
            }
            String imageStatus = imageFound ? "PASS" : "FAIL";
            
            // Verify Categories
            boolean categoryFound = false;
            String actualCategory = "Not Found";
            Cell categoryCell = row.getCell(3);
            String expectedCategory = (categoryCell != null) ? categoryCell.getStringCellValue().trim().replaceAll("[^a-zA-Z0-9]", "") : "";
            if (!expectedCategory.isEmpty()) {
                System.out.println("Verifying category: " + expectedCategory);
                List<WebElement> categories = driver.findElements(By.xpath("//*[@class='badge rounded-pill text-14']"));
                for (WebElement cat : categories) {
                    String catText = cat.getText().replaceAll("[^a-zA-Z0-9]", "");
                    if (catText.equalsIgnoreCase(expectedCategory)) {
                        categoryFound = true;
                        actualCategory = cat.getText();
                        break;
                    }
                }
            }
            String categoryStatus = categoryFound ? "PASS" : "FAIL";
            
            Row reportRow = reportSheet.createRow(i);
            reportRow.createCell(0).setCellValue(liveUrl);
            reportRow.createCell(1).setCellValue(devUrl);
            reportRow.createCell(2).setCellValue(expectedImage);
            reportRow.createCell(3).setCellValue(actualImage);
            reportRow.createCell(4).setCellValue(imageStatus);
            reportRow.createCell(5).setCellValue(expectedCategory);
            reportRow.createCell(6).setCellValue(actualCategory);
            reportRow.createCell(7).setCellValue(categoryStatus);
            
            System.out.println("Image found: " + imageFound + ", Category found: " + categoryFound + ", Image Status: " + imageStatus + ", Category Status: " + categoryStatus);
        }
        
        fis.close();
        FileOutputStream fos = new FileOutputStream(new File(reportPath));
        reportWorkbook.write(fos);
        fos.close();
        reportWorkbook.close();
        workbook.close();
        driver.quit();
        
        System.out.println("Verification process completed. Report saved at: " + reportPath);
    }
}
