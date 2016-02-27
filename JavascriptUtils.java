import org.openqa.selenium.*;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * @author <a href="https://github.com/gterre">gterre</a>
 */
public class JavascriptUtils {

    private static final String JQUERY_URL = "http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js";
    private static final int EXPLICIT_WAIT_IN_SEC = 10000;

    private static JavascriptExecutor executor(WebDriver driver) {
        if (driver instanceof JavascriptExecutor) {
            return (JavascriptExecutor) driver;
        } else {
            throw new UnsupportedOperationException("This driver does not support javascript execution");
        }
    }

    private static final ExpectedCondition<Boolean> jQueryLoaded = new ExpectedCondition<Boolean>() {
        public Boolean apply(WebDriver driver) {
            try {
                return (Boolean) executor(driver).executeScript("return jQuery()!=null");
            } catch (WebDriverException e) {
                return false;
            }
        }
    };

    /**
     * Is JQuery loaded?
     *
     * @param driver
     * @return true if JQuery is loaded, false otherwise
     */
    public static boolean isJQueryLoaded(WebDriver driver) {
        return Boolean.valueOf(jQueryLoaded.apply(driver));
    }

    /**
     * Inject JQuery if not present and wait up to the specified seconds for JQuery to load.
     *
     * @param driver  the {@link WebDriver} instance
     * @param seconds the explicit number of seconds to wait for jquery to load
     */
    public static JavascriptExecutor injectJQuery(WebDriver driver, int seconds) {
        JavascriptExecutor jsexec = executor(driver);
        if (!isJQueryLoaded(driver)) {
            WebDriverWait wait = (new WebDriverWait(driver, seconds));
            jsexec.executeScript("var headID = document.getElementsByTagName('head')[0];"
                    + "var newScript = document.createElement('script');"
                    + "newScript.type = 'text/javascript';"
                    + "newScript.src = '" + JQUERY_URL + "';"
                    + "headID.appendChild(newScript);");
            wait.until(jQueryLoaded);
        }
        return jsexec;
    }

    /**
     * What is the absolute xpath of the element.
     *
     * @param element the {@link WebElement} object;
     * @return the absolute xpath
     */
    public static String getAbsoluteXPath(WebElement element) {
        WebDriver driver = ((WrapsDriver) element).getWrappedDriver();
        return (String) executor(driver).executeScript("function absoluteXPath(element) {"
                + "var comp, comps = [];"
                + "var parent = null;"
                + "var xpath = '';"
                + "var getPos = function(element) {"
                + "var position = 1, curNode;"
                + "if (element.nodeType == Node.ATTRIBUTE_NODE) {"
                + "return null;"
                + "}"
                + "for (curNode = element.previousSibling; curNode; curNode = curNode.previousSibling) {"
                + "if (curNode.nodeName == element.nodeName) {"
                + "++position;"
                + "}"
                + "}"
                + "return position;"
                + "};"
                + "if (element instanceof Document) {"
                + "return '/';"
                + "}"
                + "for (; element && !(element instanceof Document); element = element.nodeType == Node.ATTRIBUTE_NODE ? element.ownerElement : element.parentNode) {"
                + "comp = comps[comps.length] = {};"
                + "switch (element.nodeType) {"
                + "case Node.TEXT_NODE:"
                + "comp.name = 'text()';"
                + "break;"
                + "case Node.ATTRIBUTE_NODE:"
                + "comp.name = '@' + element.nodeName;"
                + "break;"
                + "case Node.PROCESSING_INSTRUCTION_NODE:"
                + "comp.name = 'processing-instruction()';"
                + "break;"
                + "case Node.COMMENT_NODE:"
                + "comp.name = 'comment()';"
                + "break;"
                + "case Node.ELEMENT_NODE:"
                + "comp.name = element.nodeName;"
                + "break;"
                + "}"
                + "comp.position = getPos(element);"
                + "}"
                + "for (var i = comps.length - 1; i >= 0; i--) {"
                + "comp = comps[i];"
                + "xpath += '/' + comp.name.toLowerCase();"
                + "if (comp.position !== null) {"
                + "xpath += '[' + comp.position + ']';"
                + "}"
                + "}"
                + "return xpath;"
                + "} return absoluteXPath(arguments[0]);", element);
    }

    /**
     * Brings the element into the middle of the viewport (vertically), if the element is obstructed then the element
     * will be brought into the middle of the viewport. This method requires JQuery. If JQuery is not loaded,
     * this method will inject JQuery into the page and wait up to 10 seconds until loaded.
     *
     * @param element the {@link WebElement} to bring into view.
     */
    public static void bringIntoView(WebElement element) {
        WebDriver driver = ((WrapsDriver) element).getWrappedDriver();
        JavascriptExecutor jsexec = injectJQuery(driver, EXPLICIT_WAIT_IN_SEC);

        // bring in view port through selenium api, to account
        // for vertical and horizontal scrolling
        // if already in viewport this does nothing
        Coordinates coordinate = ((Locatable) element).getCoordinates();
        Point point = coordinate.inViewPort();

        if (isClickObstructed(point, element)) {
            // move to the middle of view port (vertically)
            // can be tweaked for horizontal scrolling
            jsexec.executeScript("function scrollIntoView(el) {"
                    + "var offsetTop = $(el).offset().top;"
                    + "var adjustment = Math.max(0,( $(window).height() - $(el).outerHeight(true) ) / 2);"
                    + "var scrollTop = offsetTop - adjustment;"
                    + "$('html,body').animate({"
                    + "scrollTop: scrollTop"
                    + "}, 0);"
                    + "} scrollIntoView(arguments[0]);", element);
        }
    }

    /**
     * @param point   the {@link Point} of the {@link WebElement} to click
     * @param element the {@link WebElement}
     * @return true if the click point is obstructed, false otherwise
     */
    private static boolean isClickObstructed(Point point, WebElement element) {
        WebDriver driver = ((WrapsDriver) element).getWrappedDriver();
        // find selenium's click location
        Dimension dim = element.getSize();
        int clickX = point.getX() + (dim.getWidth() / 2);
        int clickY = point.getY() + (dim.getHeight() / 2);
        // now get web element at click location
        WebElement elementAtClick = (WebElement) executor(driver).executeScript("return document.elementFromPoint(" + clickX + ", " + clickY + ");");
        String elementAtClickXPath = getAbsoluteXPath(elementAtClick);
        String elementXPath = getAbsoluteXPath(element);
        if (!elementXPath.equals(elementAtClickXPath)) {
            return true;
        }
        return false;
    }
}