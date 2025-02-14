package Blogs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BlogsDateRelatedBlogs {
    public static void main(String[] args) throws IOException, InterruptedException {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        String excelPath = "./Excel/Blogs.xlsx";
        String reportPath = "./Excel/BlogsDateRelatedBlogsReport.xlsx";

        FileInputStream fis = new FileInputStream(new File(excelPath));
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        Workbook reportWorkbook;
        Sheet reportSheet;
        File reportFile = new File(reportPath);

        if (reportFile.exists()) {
            FileInputStream reportFis = new FileInputStream(reportFile);
            reportWorkbook = new XSSFWorkbook(reportFis);
            reportSheet = reportWorkbook.getSheetAt(0);
            reportFis.close();
        } else {
            reportWorkbook = new XSSFWorkbook();
            reportSheet = reportWorkbook.createSheet("Report");
            Row header = reportSheet.createRow(0);
            String[] headers = {"Live URL", "Dev URL", "Expected Title", "Actual Title", "Title Match", "Expected Date", "Actual Date", "Date Match", "Expected H2", "Actual H2", "H2 Match", "Expected H3", "Actual H3", "H3 Match"};
            for (int j = 0; j < headers.length; j++) {
                header.createCell(j).setCellValue(headers[j]);
            }
        }

        int lastRowNum = reportSheet.getLastRowNum() + 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            String liveUrl = row.getCell(0).getStringCellValue();
            String devUrl = row.getCell(1).getStringCellValue();

            driver.get(liveUrl);
            System.out.println("Checking Live URL: " + liveUrl);
            String liveTitle = getText(driver, "//*[@class='content-holder']/h1").toLowerCase();
            String liveDate = getText(driver, "(//*[@class='details']/*)[1]").toLowerCase();
            List<String> liveH2s = convertToLowerCase(getElementsText(driver, "//*[@class='article content']//h2"));
            List<String> liveH3s = convertToLowerCase(getElementsText(driver, "//*[@class='article content']//h3"));

            driver.get(devUrl);
            System.out.println("Checking Dev URL: " + devUrl);
            String devTitle = getText(driver, "//*[@class='col-lg-8 col-xl-7']/h1").toLowerCase();
            String devDate = getText(driver, "//*[@class='text-14 post-date']").toLowerCase();
            JavascriptExecutor jse = (JavascriptExecutor)driver;
            jse.executeScript("window.scrollBy(0,950)");
            Thread.sleep(4000);
            List<String> devH2s = liveH2s.isEmpty() ? new ArrayList<>() : convertToLowerCase(getElementsText(driver, "//*[@class='col-lg-7 rich-text-content mb-5 mb-lg-0']//h2"));
            List<String> devH3s = liveH3s.isEmpty() ? new ArrayList<>() : convertToLowerCase(getElementsText(driver, "//*[@class='col-lg-7 rich-text-content mb-5 mb-lg-0']//h3"));
            boolean titleMatch = liveTitle.equals(devTitle);
            boolean dateMatch = liveDate.equals(devDate);
            boolean h2Match = liveH2s.isEmpty() || liveH2s.equals(devH2s);
            boolean h3Match = liveH3s.isEmpty() || liveH3s.equals(devH3s);
            
            System.out.println("Title match: " + titleMatch);
            System.out.println("Date match: " + dateMatch);
            System.out.println("H2 match: " + h2Match);
            System.out.println("H3 match: " + h3Match);
            
            Row reportRow = reportSheet.createRow(lastRowNum++);
            reportRow.createCell(0).setCellValue(liveUrl);
            reportRow.createCell(1).setCellValue(devUrl);
            reportRow.createCell(2).setCellValue(liveTitle);
            reportRow.createCell(3).setCellValue(devTitle);
            reportRow.createCell(4).setCellValue(titleMatch ? "TRUE" : "FALSE");
            reportRow.createCell(5).setCellValue(liveDate);
            reportRow.createCell(6).setCellValue(devDate);
            reportRow.createCell(7).setCellValue(dateMatch ? "TRUE" : "FALSE");
            reportRow.createCell(8).setCellValue(String.join(", ", liveH2s));
            reportRow.createCell(9).setCellValue(String.join(", ", devH2s));
            reportRow.createCell(10).setCellValue(h2Match ? "TRUE" : "FALSE");
            reportRow.createCell(11).setCellValue(String.join(", ", liveH3s));
            reportRow.createCell(12).setCellValue(String.join(", ", devH3s));
            reportRow.createCell(13).setCellValue(h3Match ? "TRUE" : "FALSE");

            FileOutputStream fos = new FileOutputStream(reportFile);
            reportWorkbook.write(fos);
            fos.close();
        }
        fis.close();
        driver.quit();
    }

    private static String getText(WebDriver driver, String xpath) {
        try {
            WebElement element = driver.findElement(By.xpath(xpath));
            return element != null ? element.getText().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> getElementsText(WebDriver driver, String xpath) {
        List<WebElement> elements = driver.findElements(By.xpath(xpath));
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText().trim());
        }
        return texts;
    }

    private static List<String> convertToLowerCase(List<String> texts) {
        List<String> lowerCaseTexts = new ArrayList<>();
        for (String text : texts) {
            lowerCaseTexts.add(text.toLowerCase());
        }
        return lowerCaseTexts;
    }
}

