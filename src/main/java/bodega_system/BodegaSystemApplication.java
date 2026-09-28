package bodega_system;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BodegaSystemApplication {

	public static void main(String[] args) {

		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));

		SpringApplication.run(BodegaSystemApplication.class, args);
	}

}
