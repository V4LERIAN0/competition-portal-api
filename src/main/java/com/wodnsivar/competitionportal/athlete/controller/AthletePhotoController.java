package com.wodnsivar.competitionportal.athlete.controller;

import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.athlete.service.AthleteExperienceService;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.enums.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AthletePhotoController {
  private static final String PREFIX = "/api/public/athlete-photos/";
  private final AthleteExperienceService athletes;
  private final CompetitionAthleteRepository repository;
  private final Path directory;

  public AthletePhotoController(
      AthleteExperienceService athletes,
      CompetitionAthleteRepository repository,
      @Value("${app.athlete-photo-directory:./uploads/athlete-photos}") String directory) {
    this.athletes = athletes;
    this.repository = repository;
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  @PostMapping(value = "/api/athlete/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new BadRequestException("Elige una imagen JPG o PNG de hasta 5 MB.");
    BufferedImage decoded;
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new BadRequestException("Formato de imagen no admitido. Usa JPG o PNG.");
      var reader = readers.next();
      try {
        reader.setInput(input);
        String format = reader.getFormatName();
        if (!Set.of("JPEG", "PNG").contains(format.toUpperCase(Locale.ROOT))
            || (long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000L)
          throw new BadRequestException("Usa JPG o PNG de hasta 20 megapíxeles.");
        decoded = reader.read(0);
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw new BadRequestException("La imagen no es válida. Usa otro archivo JPG o PNG.");
    }
    double scale = Math.min(1, 1024.0 / Math.max(decoded.getWidth(), decoded.getHeight()));
    var output =
        new BufferedImage(
            Math.max(1, (int) (decoded.getWidth() * scale)),
            Math.max(1, (int) (decoded.getHeight() * scale)),
            BufferedImage.TYPE_INT_RGB);
    var graphics = output.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.drawImage(decoded, 0, 0, output.getWidth(), output.getHeight(), null);
    graphics.dispose();
    String name = UUID.randomUUID() + ".jpg";
    Files.createDirectories(directory);
    Path path = directory.resolve(name);
    ImageIO.write(output, "jpg", path.toFile());
    try {
      athletes.setPhoto(PREFIX + name);
    } catch (RuntimeException exception) {
      Files.deleteIfExists(path);
      throw exception;
    }
    return Map.of("url", PREFIX + name);
  }

  @DeleteMapping("/api/athlete/me/photo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove() {
    athletes.setPhoto(null);
  }

  @GetMapping("/api/public/athlete-photos/{name}")
  public ResponseEntity<FileSystemResource> photo(@PathVariable String name) {
    if (!name.matches("[a-f0-9-]{36}\\.jpg"))
      throw new ResourceNotFoundException("Imagen no encontrada.");
    var athlete =
        repository
            .findByProfilePhotoUrl(PREFIX + name)
            .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada."));
    var competition = athlete.getCompetition();
    if (competition.getVisibilityStatus() != VisibilityStatus.PUBLIC
        || competition.getStatus() == CompetitionStatus.DRAFT
        || competition.getStatus() == CompetitionStatus.ARCHIVED
        || athlete.getStatus() == AthleteStatus.WITHDRAWN
        || athlete.getStatus() == AthleteStatus.DISQUALIFIED)
      throw new ResourceNotFoundException("Imagen no encontrada.");
    var file = new FileSystemResource(directory.resolve(name));
    if (!file.exists()) throw new ResourceNotFoundException("Imagen no encontrada.");
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(file);
  }
}
