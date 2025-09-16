	package ca.sheridancollege.dobariyz.util;
		
	import java.util.Date;
	import java.security.Key;
	import org.springframework.stereotype.Component;
	import javax.crypto.spec.SecretKeySpec;
	import io.jsonwebtoken.Claims;
	import io.jsonwebtoken.Jwts;
	import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
	
	
	@Component
	public class JwtUtil {
		private String secretKey = "QW5kIHRoZW4gd2Ugc3JpdGUgc29tdyBsb25nIHNjcmlwdCBoZXJlIHRoYXQgaXMgZ29pbmcgdG8gc2VydmUgYXMgc29tZXRoaW5nIHRvIGVuY29kZSB3aGljaCBpcyBoYXJkIHRvIGRlY29kZQ"; // You can store this in application.properties
	
		
		public String generateToken(String email) {
		   // System.out.println("Secret Key Used for Signing: " + secretKey);
	
		    return Jwts.builder()
		            .setSubject(email)
		            .setIssuedAt(new Date())
		            .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 500)) // 500 hours expiration
		            .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
		            .compact();
		}
	
		public Claims extractClaims(String token) {
		    // Convert the secret key into a Key object
		    Key key = new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName());
	
		    return Jwts.parserBuilder()
		            .setSigningKey(key)  // Use the Key object here
		            .build()
		            .parseClaimsJws(token)
		            .getBody();
		}
	
	    public String extractEmail(String token) {
	        return extractClaims(token).getSubject();
	    }
	
	    public boolean isTokenExpired(String token) {
	        return extractClaims(token).getExpiration().before(new Date());
	    }
	
	    public boolean validateToken(String token, String email) {
	      //  System.out.println("Secret Key Used for Validation: " + secretKey);
	        String extractedEmail = extractEmail(token);
	       // System.out.println("Extracted Email: " + extractedEmail);

	        return extractedEmail.equals(email);
	    }
	}
