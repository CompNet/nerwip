package tr.edu.gsu.nerwip.recognition.external.nero;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.sun.xml.internal.messaging.saaj.util.Base64;

import tr.edu.gsu.nerwip.data.article.Article;
import tr.edu.gsu.nerwip.data.article.ArticleLanguage;
import tr.edu.gsu.nerwip.data.entity.EntityType;
import tr.edu.gsu.nerwip.recognition.RecognizerException;
import tr.edu.gsu.nerwip.recognition.RecognizerName;
import tr.edu.gsu.nerwip.recognition.external.AbstractExternalRecognizer;
import tr.edu.gsu.nerwip.recognition.internal.modelless.AbstractModellessInternalRecognizer;
import tr.edu.gsu.nerwip.tools.keys.KeyHandler;

/**
 * This class acts as an interface with Nero. <br/>
 * Recommended parameter values:
 * <ul>
 * <li>{@code ignorePronouns}: {@code true}</li>
 * <li>{@code exclusionOn}: {@code false}</li>
 * </ul>
 * Official Nero website: <a
 * href="https://nero.irisa.fr/">https://nero.irisa.fr/</a>
 * 
 * @author Sabrine Ayachi
 * @author Vincent Labatut
 */
public class Nero extends AbstractExternalRecognizer<NeroConverter> {
	/**
	 * Builds and sets up an object representing NERO tool.
	 * 
	 * @param ignorePronouns
	 *            Whether or not prnouns should be excluded from the detection.
	 * @param exclusionOn
	 *            Whether or not stop words should be excluded from the
	 *            detection.
	 */

	public Nero(boolean ignorePronouns, boolean exclusionOn) {
		super(false, ignorePronouns, exclusionOn);

		// init converter
		converter = new NeroConverter(getFolder());
	}

	// ///////////////////////////////////////////////////////////////
	// NAME /////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////
	@Override
	public RecognizerName getName() {
		return RecognizerName.NERO;
	}

	// ///////////////////////////////////////////////////////////////
	// FOLDER /////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////
	@Override
	public String getFolder() {
		String result = getName().toString();

		result = result + "_" + "ignPro=" + ignorePronouns;
		result = result + "_" + "exclude=" + exclusionOn;

		return result;
	}

	// ///////////////////////////////////////////////////////////////
	// ENTITY TYPES /////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////
	/** List of entities recognized by Nero */
	private static final List<EntityType> HANDLED_TYPES = Arrays.asList(
			EntityType.DATE, EntityType.LOCATION, EntityType.ORGANIZATION,
			EntityType.FUNCTION, EntityType.PERSON);

	@Override
	public List<EntityType> getHandledEntityTypes() {
		return HANDLED_TYPES;
	}

	// ///////////////////////////////////////////////////////////////
	// LANGUAGES /////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////
	/** List of languages this recognizer can treat */
	private static final List<ArticleLanguage> HANDLED_LANGUAGES = Arrays
			.asList(ArticleLanguage.EN, ArticleLanguage.FR);

	@Override
	public boolean canHandleLanguage(ArticleLanguage language) {
		boolean result = HANDLED_LANGUAGES.contains(language);
		return result;
	}

	// ///////////////////////////////////////////////////////////////
	// PROCESSING /////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////
	/** Key name for Nero */
	public static final String KEY_NAME = "Nero";

	@Override
	protected String detectEntities(Article article) throws RecognizerException
	{	String text = article.getRawText();
		String result = new String();
		String res1 = new String();
		// check if the id was set
		String id = KeyHandler.IDS.get(KEY_NAME);
		if (id == null)
			throw new NullPointerException(
					"In order to use Nero, you first need to set up your id in file res/misc/keys.xml using the exact name \"Nero\".");

		// check if the key was set
		String key = KeyHandler.KEYS.get(KEY_NAME);
		if (key == null)
			throw new NullPointerException(
					"In order to use Nero, you first need to set up your user key in file res/misc/keys.xml using the exact name \"Nero\".");
		try { // define HTTP message
			byte[] encodedBytes = Base64.encode((id + ":" + key).getBytes());
			String encoding = new String(encodedBytes);
			logger.log("Build Nero first request");
			String url = "https://nero.irisa.fr/texts.xml";
			HttpPost httpPost = new HttpPost(url);
			httpPost.setHeader("Authorization", "Basic " + encoding);
			List<NameValuePair> Params = new ArrayList<NameValuePair>();
			Params.add(new BasicNameValuePair("text[content]", text));
			httpPost.setEntity(new UrlEncodedFormEntity(Params));

			// send to nero
			logger.log("Send first message to Nero");
			HttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(httpPost);
			int responseCode = response.getStatusLine().getStatusCode();

			// read answer
			// logger.log("Read Nero first answer");
			logger.log("Response Code : " + responseCode);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			StringBuffer res = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				logger.log(line);
				res.append(line);
			}
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(new StringReader(res.toString()));
			Element root = doc.getRootElement();
			Element idElt = root.getChild("id");
			String identifier = idElt.getValue();

			// second request
			int i = 1;
			boolean again;
			do {
				logger.log("\nRepetition " + i);
				again = false;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// logger.log("Send second message to Nero");
				byte[] encodedBytes1 = Base64.encode((id + ":" + key)
						.getBytes());
				String encoding1 = new String(encodedBytes1);
				url = "https://nero.irisa.fr/texts/" + identifier + ".xml";
				logger.log("url=" + url);
				HttpGet httpGet = new HttpGet(url);
				httpGet.setHeader("Authorization", "Basic " + encoding1);

				client = new DefaultHttpClient();
				response = client.execute(httpGet);
				responseCode = response.getStatusLine().getStatusCode();
				logger.log("Response Code : " + responseCode);
				if (responseCode != 200) {
					again = true;
					logger.log("WARNING: received an error code ("
							+ responseCode + ") >> trying again");
				}
				rd = new BufferedReader(new InputStreamReader(response
						.getEntity().getContent(), "UTF-8"));
				res = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					logger.log(line);
					res.append(line);
					if (line.contains("<result nil=\"true\"")) {
						again = true;
						logger.log("WARNING: the result part was empty >> trying again");
					}
				}
				i++;
			} while (again);

			// logger.log("extracting result");
			logger.log("tester");

			// String originalText =
			// "Né le 19 juillet 1876 au Mans (Sarthe), mort le 29 novembre 1926 à Paris";
			// String NeroAnswer =
			// "N <time> le dix-neuf juillet mille huit cent soixante-seize </time> au <loc> Mans </loc> (<loc>Sarthe </loc>), mort <time> le vingt-neuf novembre mille neuf cent vingt-six </time> à <loc> Paris </loc>";
			// String res1 = new String();
			// build DOM
			logger.log("Build DOM");
			String NeroAnswer = new String();
			result = res.toString();
			sb = new SAXBuilder();
			doc = sb.build(new StringReader(result));
			root = doc.getRootElement();

			Element resultElt = root.getChild("result");
			NeroAnswer = resultElt.getValue();
			logger.log(">>>>extracting result");
			logger.log(">>>>result = " + NeroAnswer);
			String originalText = article.getRawText();
			logger.log(">>>>originalText = " + originalText);
			char co = originalText.charAt(0);
			int i1 = 0;
			char cr = NeroAnswer.charAt(0);
			int j = 0;
			do {
				co = originalText.charAt(i1);
				cr = NeroAnswer.charAt(j);
				if (co == cr) {
					logger.log("case 1");
					res1 = res1 + co;
					logger.log("res1 :" + res1);
					i1++;
					j++;
				} else if (DiacriticalChar(co) == false && cr == ' ') {
					logger.log("case 2");
					res1 = res1 + co;
					logger.log("res1 :" + res1);
					i1++;
				} else if (cr == '<')
					if (NeroAnswer.charAt(i1 + 1) != '/') {
						logger.log("case 3");
						String word = funct(j, NeroAnswer);
						if (res1.charAt(res1.length() - 1) == ' ') {
							res1 = res1.substring(0, res1.length() - 1);
						}
						res1 = res1 + word + '>';
						logger.log("res1 :" + res1);
						j = j + word.length() + 1;
						if (NeroAnswer.charAt(j) == ' ') {
							logger.log("case 4");
							j++;
							logger.log("res1 :" + res1);
						}
					} else if (NeroAnswer.charAt(j + 1) == '/') {
						logger.log("case 5");
						if (res1.charAt(res1.length() - 1) == ' ') {
							res1 = res1.substring(0, res1.length() - 1);
							i1--;
						}
						String word1 = funct(j, NeroAnswer);
						res1 = res1 + word1 + '>';
						j = j + word1.length() + 1;
						logger.log("res1 :" + res1);
						/*
						 * if ( NeroAnswer.charAt(j-1) == ' ' ) {
						 * logger.log("case 5"); i1--; String word1 = funct(j,
						 * NeroAnswer); res1 = res1 + word1 + '>';
						 * logger.log("res1 :" + res1 ); }
						 */
					} else
						logger.log("error1");
				else if (DiacriticalChar(co) == true && cr == ' ') {
					logger.log("case 6");
					j++;
					logger.log("res1 :" + res1);
				} else if (IsNumber(co) == true) {
					logger.log("case 7");
					String word2 = PassNumber(i1, originalText);
					res1 = res1 + word2;
					i1 = i1 + word2.length();
					String word3 = PassNumeric(j, NeroAnswer);
					j = j + word3.length() + 1;
					logger.log("res1 :" + res1);
				} else {
					logger.log("error2");
					System.exit(0);
				}
				logger.log("i1:" + i1);
				logger.log("j:" + j);
			} while (i1 <= originalText.length());

			// String regex =
			// "<loc> | </loc> | <pers> | </pers> | <org> | </org> | <fonc> | </fonc> | <time> |</time>";
			String regex = "<(.*)>";
			res1.replaceAll(regex, " ");
			boolean equal = res1.equals(originalText);
			logger.log("equal : " + equal);
		}

		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RecognizerException(e.getMessage());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			throw new RecognizerException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RecognizerException(e.getMessage());
		} catch (JDOMException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		logger.log(">>>writing result file");
		return res1;
	}

	/**
	 * Receives a character and return if it's a diacritical character
	 * 
	 * @param c
	 *            character to process.
	 * @return boolean result.
	 */
	public boolean DiacriticalChar(char c)
	{	String oldC = Character.toString(c);
		String newC = StringUtils.stripAccents(oldC);
		boolean result = oldC.equals(newC);
		return result; // TODO
	}

	@SuppressWarnings("javadoc")
	public String funct(int i, String ch)
	{	String result = new String();
		do {
			result = result + ch.charAt(i);
			i++;

		} while (ch.charAt(i) != '>');
		return result;

	}

	/**
	 * Receives a character and return if it's a number
	 * 
	 * @param c
	 *            character to process.
	 * @return boolean result.
	 */
	public boolean IsNumber(char c)
	{	char[] letters = new char[] { '0', '1', '2', '3', '4', '5', '6', '7',
				'8', '9' };
		for (char x : letters) {
			if (x == c) {
				return true;
			}
		}
		return false;
	}

	// //passer le nombre
	@SuppressWarnings("javadoc")
	public String PassNumber(int i, String ch)
	{	String result = new String();
		do {
			result = result + ch.charAt(i);
		} while (IsNumber(ch.charAt(i)) == true);
		return result;
	}

	/**
	 * Receives a postion i and a string and return a string composed of the
	 * digital words existing from the i position.
	 * 
	 * @param i
	 *            position of the character to start process with.
	 * @return the digital string.
	 */
	public String PassNumeric(int i, String ch)
	{	String result = new String();
		do {
			result = result + ch.charAt(i);
		} while (ch.charAt(i) != '>');
		return result;
	}

	/*
	 * public static String Mois(int i, String ch) { String result = new
	 * String(); char[] letters = new char[] { 'j', 'f', 'm', 'a', 's', 'o',
	 * 'n', 'd'}; List<String> mois = Arrays.asList("janvier", "février",
	 * "mars", "avril", "mai", "juin", "juillet", "aout", "septembre",
	 * "octobre", "novembre", "décembre"); for (char x : letters) { if (x ==
	 * ch.charAt(i)) { do { result = result + ch.charAt(i); i++; } while (
	 * !mois.contains(result)); } } return result; }
	 */
}
