package com.orthodox.orthodoxapp.account;

import com.orthodox.orthodoxapp.auth.AuthException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProfilePhotoService: checks, shrinks and stores profile photos.
 *
 * WHY NOT JUST SAVE THE FILE?
 * An uploaded "photo" is just bytes from the internet. It might not be a
 * picture at all, it might be huge, or it might carry hidden data: phone
 * photos often include the GPS location where they were taken. So we:
 *   1. read it as an image (anything that isn't a real JPEG or PNG is refused)
 *   2. refuse giant images before decoding them (a tiny file can claim to be
 *      50,000 x 50,000 pixels and use up all the server's memory)
 *   3. crop the centre to a square and shrink it to 256 x 256
 *   4. save a brand-new JPEG we drew ourselves: no hidden data survives
 *
 * The account page also crops and shrinks the photo in the browser before
 * uploading (which handles phone photos taken sideways), but the server never
 * trusts the browser, so it checks everything again.
 */
@Service
public class ProfilePhotoService {

    static final int SIZE = 256;
    private static final int MAX_SOURCE_SIDE = 8000;
    private static final Set<String> FORMATS = Set.of("jpeg", "png");

    private final ProfilePhotoRepository photos;
    private final AppUserRepository users;
    private final AccountService accountService;

    public ProfilePhotoService(ProfilePhotoRepository photos, AppUserRepository users, AccountService accountService) {
        this.photos = photos;
        this.users = users;
        this.accountService = accountService;
    }

    @Transactional
    public AppUser save(Long userId, byte[] upload) {
        AppUser user = accountService.get(userId);
        photos.save(new ProfilePhoto(user.getId(), toSquareJpeg(upload)));
        user.setPhotoUpdatedAt(Instant.now());
        return users.save(user);
    }

    public Optional<byte[]> find(Long userId) {
        return photos.findById(userId).map(ProfilePhoto::getJpeg);
    }

    @Transactional
    public AppUser delete(Long userId) {
        AppUser user = accountService.get(userId);
        photos.deleteById(user.getId());
        user.setPhotoUpdatedAt(null);
        return users.save(user);
    }

    /** Steps 1-4 above. "static" because it only works on the bytes it's given. */
    static byte[] toSquareJpeg(byte[] upload) {
        BufferedImage source = readImage(upload);

        // 3. The biggest square that fits, taken from the middle.
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage square = source.getSubimage(
                (source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);

        // 4. Draw it onto a new, blank 256 x 256 picture. A white background
        //    fills any see-through parts of a PNG, since JPEG can't store them.
        BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = result.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        canvas.setColor(Color.WHITE);
        canvas.fillRect(0, 0, SIZE, SIZE);
        canvas.drawImage(square, 0, 0, SIZE, SIZE, null);
        canvas.dispose();

        try {
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            ImageIO.write(result, "jpg", jpeg);
            return jpeg.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't save the photo as JPEG", ex);
        }
    }

    // Steps 1 and 2: an ImageReader can tell us the format and size BEFORE
    // decoding all the pixels.
    private static BufferedImage readImage(byte[] upload) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw notAPhoto();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw notAPhoto();
                }
                if (reader.getWidth(0) > MAX_SOURCE_SIDE || reader.getHeight(0) > MAX_SOURCE_SIDE) {
                    throw new AuthException(HttpStatus.BAD_REQUEST,
                            "That photo is too big. Please use one under " + MAX_SOURCE_SIDE + " pixels wide and high.");
                }
                return reader.read(0);
            } finally {
                reader.dispose(); // free the reader's memory
            }
        } catch (IOException ex) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "We couldn't read that photo. Please try a different JPEG or PNG.");
        }
    }

    private static AuthException notAPhoto() {
        return new AuthException(HttpStatus.BAD_REQUEST, "Please choose a JPEG or PNG photo.");
    }
}
