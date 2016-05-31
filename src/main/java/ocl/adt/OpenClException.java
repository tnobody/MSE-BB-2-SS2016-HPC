package ocl.adt;

/**
 * Created by tim on 25.05.2016.
 */
public class OpenClException extends Exception {
    public OpenClException(ErrorFlags error) {
        super("An OpenClError occured: " + error.toString());
    }
}
