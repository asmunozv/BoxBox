module boxbox {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.media;
	requires java.sql;
	requires java.net.http;
	requires com.google.gson;
	
	opens application to javafx.graphics, javafx.fxml;
	opens controller to javafx.fxml;
	opens model.api to com.google.gson;
	opens model.entity to javafx.base;
	opens view to com.google.gson, javafx.fxml;
}
