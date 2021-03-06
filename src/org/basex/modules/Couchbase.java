package org.basex.modules;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.internal.OperationFuture;

import org.basex.modules.errors.CouchbaseErrors;
import org.basex.query.QueryException;
import org.basex.query.func.FuncOptions;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.QNm;
import org.basex.query.value.item.Str;
import org.basex.query.value.map.Map;
import org.basex.query.value.type.SeqType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.couchbase.client.vbucket.ConfigurationException;


/**
 * CouchBase extension of Basex.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Prakash Thapa
 */
public class Couchbase extends Nosql {
    protected static final String DESCENDING = "descending";
    protected static final String ENDKEY = "endkey";
    protected static final String GROUP = "group";
    protected static final String GROUP_LEVEL = "group_level";
    protected static final String KEY = "key";
    protected static final String KEYS = "keys";
    protected static final String LIMIT = "limit";
    protected static final String REDUCE = "reduce";
    protected static final String SKIP = "skip";
    protected static final String STALE = "stale";
    protected static final String STARTKEY = "startkey";
    protected static final String DEBUG = "debug";
    protected static final String VIEWMODE = "viewmode";
    protected static final String OK = "ok";
    protected static final String FALSE = "false";
    protected static final String UPDATE_AFTER = "update_after";
    protected static final String RANGE = "range";
    protected static final String VALUEONLY = "valueonly";
    protected static final String INCLUDEDOCS = "includedocs";
    protected boolean valueOnly;
    /** URL of this module. */
    protected static final String COUCHBASE_URL = "http://basex.org/modules/couchbase";
    /** QName of Couchbase options. */
    protected static final QNm Q_COUCHBASE = QNm.get("couchbase", "options",
            COUCHBASE_URL);
    /** Couchbase instances. */
    protected HashMap<String, CouchbaseClient> couchbaseclients =
            new HashMap<String, CouchbaseClient>();
    /** Couchbase options. */
    protected HashMap<String, NosqlOptions> couchopts = new HashMap
            <String, NosqlOptions>();
    /** nodes array. */
    //protected ArrayList<URI> nodes = new ArrayList<URI>();
    public Couchbase() {
        super(Q_COUCHBASE);
        this.valueOnly = false;
    }
    /**
     * Couchbase connection with url host bucket.
     * @param url
     * @param bucket
     * @param password
     * @return Connection handler of Couchbase url
     * @throws Exception
     */
    public Str connection(final Str url, final Str bucket, final Str password)
            throws Exception {
        return connection(url, bucket, password, null);
    }
    /**
     * Couchbase connection with url host bucket and with option.
     * @param url
     * @param bucket
     * @param password
     * @return Connection handler of Couchbase url
     * @throws Exception
     */
    public Str connection(final Str url, final Str bucket, final Str password,
            final Map options) throws Exception {
        final NosqlOptions opts = new NosqlOptions();
        if(options != null) {
            new FuncOptions(Q_COUCHBASE, null).parse(options, opts);
        }
        try {
            String handler = "cbClient" + couchbaseclients.size();
            List<URI> hosts = Arrays.asList(new URI(url.toJava()));
//            CouchbaseClient client = new CouchbaseClient(hosts,
//                    bucket.toJava(), password.toJava());
            CouchbaseConnectionFactory cf = new CouchbaseConnectionFactory(hosts,
                    bucket.toJava(), password.toJava());
            CouchbaseClient client    =   new CouchbaseClient(cf);
            if(options != null) {
                couchopts.put(handler, opts);
            }
            couchbaseclients.put(handler, client);
            return Str.get(handler);
        } catch (ConfigurationException e) {
            throw CouchbaseErrors.unAuthorised();
        }catch (Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
          }
    }
    /**
     * get CouchbaseClinet from the hashmap.
     * @param handler
     * @return connection instance.
     * @throws QueryException
     */
    protected CouchbaseClient getClient(final Str handler) throws QueryException {
        String ch = handler.toJava();
        try {
            final CouchbaseClient client = couchbaseclients.get(ch);
            if(client == null)
                throw CouchbaseErrors.couchbaseClientError(ch);
            return client;
        } catch (final Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
        }
    }
    /**
     * get Couchbase option from particular db handler.
     * @param handler
     * @return MongoOptions
     */
    protected NosqlOptions getCouchbaseOption(final Str handler) {
        NosqlOptions opt = couchopts.get(handler.toJava());
        if(opt != null)
            return opt;
        else
            return null;
    }
    /**
     * This will check the assigned options and then return the final result
     * process by parent class.
     * @param handler
     * @param json
     * @return
     * @throws Exception
     */
    protected Item returnResult(final Str handler, final Str json)
            throws Exception {
        final NosqlOptions opt =   getCouchbaseOption(handler);
        Str j = json;
        if(j == null) {
            j =  Str.get("{}");
        }
        if(opt != null) {
            return finalResult(j, opt);
        } else {
            return finalResult(j, null);
        }
    }
    /**
     * add new document.
     * @param handler Database handler
     * @param key
     * @param value
     * @throws QueryException
     */
    public Item add(final Str handler, final Item key, final Item doc)
            throws QueryException {
        return put(handler, key, doc, "add");
    }
    /**
     * Set method of Couchbase.
     * @param handler
     * @param key
     * @param doc
     * @return
     * @throws QueryException
     */
    public Item set(final Str handler, final Item key,  final Item doc)
            throws QueryException {
        return put(handler, key, doc, "set");
    }
    /**
     * Replace method of Couchbase document with condition.
     * @param handler
     * @param key
     * @param doc
     * @return Item
     * @throws QueryException
     */
    public Item replace(final Str handler, final Item key, final Item doc)
            throws QueryException {
       return put(handler, key, doc, "replace");
    }
    /**
     * Append document with key.
     * @param handler
     * @param key
     * @param doc
     * @return Item
     * @throws QueryException
     */
    public Item append(final Str handler, final Item key, final Item doc)
            throws QueryException {
        return put(handler, key, doc, null);
    }
    /**
     * document addition.
     * @param handler
     * @param key
     * @param doc
     * @param options {set,put,append,replace}
     * @return Item
     * @throws QueryException
     */
    public Item put(final Str handler, final Item key, final Item doc,
            final String type) throws QueryException {
        CouchbaseClient client = getClient(handler);
        OperationFuture<Boolean> result = null;
        try {
            if(type != null) {
                if(type.equals("add")) {
                 result = client.add(
                           itemToString(key), itemToJsonString(doc));
                } else if(type.equals("replace")) {
                    result = client.replace(
                           itemToString(key), itemToJsonString(doc));
                } else if(type.equals("set")) {
                   result = client.set(
                           itemToString(key), itemToJsonString(doc));
                } else {
                   result = client.append(
                           itemToString(key), itemToString(doc));
                }
            } else {
                result = client.append(
                        itemToString(key), itemToString(doc));
                //return append(handler, key, doc);
            }
            String msg = result.getStatus().getMessage();
            if(result.get().booleanValue()) {
                return Str.get(msg);
            } else {
                throw CouchbaseErrors.couchbaseOperationFail(type, msg);
            }
        } catch (Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
        }
    }
    /**
     * get document by key.
     * @param handler
     * @param key
     * @return Item
     * @throws QueryException
     */
    public Item get(final Str handler, final Item key) throws QueryException {
        CouchbaseClient client = getClient(handler);
        try {
            Object result =  client.get(itemToString(key));
            if(result == null) {
                result = "{}";
            }
            Str json = Str.get((String) result);
            return returnResult(handler, json);
        } catch (Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
        }
    }
    /**
     * Get document with options.
     * @param handler
     * @param doc
     * @param options
     * @return
     * @throws QueryException
     */
    public Item get(final Str handler, final Str doc, final Map options)
         throws QueryException {
        CouchbaseClient client = getClient(handler);
        try {
            if(options != null) {
                Value keys = options.keys();
                for(final Item key : keys) {
                    if(!(key instanceof Str))
                        throw CouchbaseErrors.
                        couchbaseMessageOneKey("String value expected for '%s' key ",
                                key.toJava());
                    final String k = ((Str) key).toJava();
                    final Value v = options.get(key, null);
                    if(k.equals("add")) {
                        if(v.type().instanceOf(SeqType.STR)) {
                            Str s = (Str) v.toJava();
                            return s;
                        }
                    }
                }
            }
            Object json = client.get(doc.toJava());
            if(json == null) {
                json = "{}";
            }
            Str jsonStr = Str.get((String) json);
            return returnResult(handler, jsonStr);
        } catch (Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
        }
    }
    public Item getbulk(final Str handler, final Value keyItems) throws QueryException {
        CouchbaseClient client = getClient(handler);
         try {
             if(keyItems.size() < 1) {
                 throw CouchbaseErrors.keysetEmpty();
             }
             List<String> keys = new ArrayList<String>();
             for (Value v: keyItems) {
                String s = (String) v.toJava();
                keys.add(s);
             }
             java.util.Map<String, Object> bulkset = client.getBulk(keys);
             Str json = getBulkJson(bulkset);
             return returnResult(handler, json);
         } catch (Exception ex) {
            throw new QueryException(ex);
        }
    }
    /**
     * Process Java Map<String, Object> (key/value), and return JSON Str.
     * @param bulkset java Map<String Object> for key and value set
     * @return json (STR)
     */
    protected Str getBulkJson(final java.util.Map<String, Object> bulkset) {
        final StringBuilder json = new StringBuilder();
        json.append("{ ");
        for (String key: bulkset.keySet()) {
            if(json.length() > 2) json.append(", ");
            json.append('"').append(key).append('"').append(" : ");
            String value = (String) bulkset.get(key);
            if(value != null) {
                value = value.trim();
                if(value.charAt(0) == '{' || value.charAt(0) == '[') {
                    json.append(value);
                } else {
                    json.append('"').append(value.replaceAll("\"", "\\\"")).append('"');;
                }
            } else {
                json.append('"').append("").append('"');
            }
        }
        json.append(" } ");
        return Str.get(json.toString());
    }
    /**
     * remove document by key.
     * @param handler
     * @param key
     * @return
     * @throws QueryException
     */
    public Item remove(final Str handler, final Str key) throws QueryException {
        return delete(handler, key);
    }
    /**
     * Delete document by key.
     * @param handler
     * @param key
     * @return
     * @throws QueryException
     */
    public Item delete(final Str handler, final Str key) throws QueryException {
        CouchbaseClient client = getClient(handler);
        try {
            OperationFuture<Boolean> result = client.delete(key.toJava());
            String msg = result.getStatus().getMessage();
            if(result.get().booleanValue()) {
                return Str.get(msg);
            } else {
                throw CouchbaseErrors.couchbaseOperationFail("delete", msg);
            }
        } catch (Exception ex) {
            throw CouchbaseErrors.generalExceptionError(ex);
        }
    }
    /**
     * Create view without reduce method.
     * @param handler
     * @param doc
     * @param viewName
     * @param map
     * @return
     * @throws QueryException
     */
    public Item createView(final Str handler, final Str doc, final Str viewName,
            final Str map) throws QueryException {
        return createView(handler, doc, viewName, map, null);
    }
    /**
     * Create view with reduce method.
     * @param handler Database handler
     * @param doc
     * @param viewName
     * @param map
     * @param reduce
     * @return
     * @throws QueryException
     */
    public Item createView(final Str handler, final Str doc, final Str viewName,
            final Str map, final Str reduce) throws QueryException {
        CouchbaseClient client = getClient(handler);
        if(map == null) {
            throw CouchbaseErrors.generalExceptionError("map function is empty");
        }
        try {
            DesignDocument designDoc = new DesignDocument(doc.toJava());
            ViewDesign viewDesign;
            if(reduce != null) {
               viewDesign = new ViewDesign(viewName.toJava(),
                       map.toJava(), reduce.toJava());
            } else {
                viewDesign = new ViewDesign(viewName.toJava(), map.toJava());
            }
            designDoc.getViews().add(viewDesign);
           boolean success = client.createDesignDoc(designDoc);
           if(success) {
               return Str.get("ok");
           } else {
               final String msg = "There is something wrong when creating View";
               throw CouchbaseErrors.generalExceptionError(msg);
           }
        } catch (Exception e) {
            throw CouchbaseErrors.generalExceptionError(e);
        }
    }
    /**
     * Get data from view without any option.
     * @param handler
     * @param doc
     * @param viewName
     * @return item
     * @throws QueryException
     */
    public Item getview(final Str handler, final Str doc, final Str viewName)
            throws QueryException {
        return getview(handler, doc, viewName, null);
    }
    /**
     * Map.
     * @param options
     * @return
     * @throws QueryException
     */
    protected Query query(final Map options) throws QueryException {
        Query q = new Query();
        if(options != null) {
            Value keys = options.keys();
            for(final Item key : keys) {
                if(!(key instanceof Str))
                    throw CouchbaseErrors.
                    couchbaseMessageOneKey("String value expected for '%s' key ",
                            key.toJava());
                final String k = ((Str) key).toJava();
                final Value v = options.get(key, null);
                if(k.equals(VIEWMODE)) {
                    System.setProperty(VIEWMODE, v.toJava().toString());
                } else if(k.equals(LIMIT)) {
                    if(v.type().instanceOf(SeqType.ITR_OM)) {
                        long l = ((Item) v).itr(null);
                        q.setLimit((int) l);
                    } else {
                        throw CouchbaseErrors.
                        couchbaseMessageOneKey("Integer value expected for '%s' key ",
                                key.toJava());
                    }
                } else if(k.equals(STALE)) {
                    String s = ((Item) v).toString();
                    if(s.equals(OK))
                        q.setStale(Stale.OK);
                    else if(s.equals(FALSE))
                        q.setStale(Stale.FALSE);
                    else if(s.equals(UPDATE_AFTER))
                        q.setStale(Stale.UPDATE_AFTER);
                } else if(k.equals(KEY)) {
                    q.setKey(((Item) v).toString());
                } else if(k.equals(DESCENDING)) {
                    boolean desc = ((Item) v).bool(null);
                    q.setDescending(desc);
                } else if(k.equals(DEBUG)) {
                    q.setDebug(((Item) v).bool(null));
                } else if(k.equals(REDUCE)) {
                    boolean d = ((Item) v).bool(null);
                    q.setReduce(d);
                } else if(k.equals(GROUP)) {
                    boolean d = ((Item) v).bool(null);
                    q.setGroup(d);
                } else if(k.equals(STARTKEY)) {
                    String s = ((Str) v).toJava();
                    q.setStartkeyDocID(s);
                } else if(k.equals(ENDKEY)) {
                    String s = ((Item) v).toString();
                    q.setEndkeyDocID(s);
                } else if(k.equals(SKIP)) {
                    if(v.type().instanceOf(SeqType.ITR_OM)) {
                        long l = ((Item) v).itr(null);
                        q.setSkip((int) l);
                    } else {
                        throw CouchbaseErrors.
                        couchbaseMessageOneKey("Integer value expected for '%s' key ",
                                key.toJava());
                    }
                } else if(k.equals(GROUP_LEVEL)) {
                    if(v.type().instanceOf(SeqType.ITR_OM)) {
                        long l = ((Item) v).itr(null);
                        q.setGroupLevel((int) l);
                    } else {
                        throw CouchbaseErrors.
                        couchbaseMessageOneKey("Integer value expected for '%' key ",
                                key.toJava());
                    }
                } else if(k.equals(RANGE)) {
                    if(!(v instanceof Map)) {
                        throw CouchbaseErrors.
                        couchbaseMessageOneKey(" Map is expected for key '%'",
                                key.toJava());
                    }
                    Map range = (Map) v;
                    Value s = range.get(Str.get(STARTKEY), null);
                    Value e = range.get(Str.get(ENDKEY), null);
                    String msg = (s == null) ? " 'startkey' is empty" : (e == null) ?
                            "'endkey' is empty" : null;
                    if(msg != null) {
                        throw CouchbaseErrors.
                        generalExceptionError(msg);
                    }
                    q.setRange(ckey(s), ckey(s));
                } else if(k.toLowerCase().equals(KEYS)) {
                    ComplexKey ckey = ckey(v);
                   if(ckey != null) {
                       q.setKeys(ckey);
                   }
                } else if(k.toLowerCase().equals(VALUEONLY)) {
                    valueOnly = ((Item) v).bool(null);
                } else if(k.toLowerCase().equals("solution")) {
                    ((Item) v).bool(null);
                } else if(k.toLowerCase().equals(INCLUDEDOCS)) {
                    q.setIncludeDocs(true);
                }
            }
        }
        return q;
    }
    /**
     * view with mode Option.
     * @param handler
     * @param doc
     * @param viewName
     * @param mode
     * @param options options like limit and so on(not completed)
     * @return
     * @throws QueryException
     */
    public Item getview(final Str handler, final Str doc, final Str viewName,
            final Map options) throws QueryException {
        final CouchbaseClient client = getClient(handler);
        Query q = this.query(options);
        try {
            View view = client.getView(doc.toJava(), viewName.toJava());
            ViewResponse response = client.query(view, q);
            Str json = valueOnly ? viewResponseToJsonValueOnly(response)
                    : viewResponseToJson(response);
            return returnResult(handler, json);
        } catch (Exception e) {
            throw CouchbaseErrors.generalExceptionError(e);
        }
    }
    /**
     * convert sequence into couchbase's java complexkey.
     * @param v sequence
     * @return ComplexKey
     * @throws QueryException
     */
    protected ComplexKey ckey(final Value v) throws QueryException {
        if(v.size() <= 0) {
            throw CouchbaseErrors.
            couchbaseMessageOneKey("items for '%s' cannot be empty",
                    v.toJava());
        }
        Object [] keys = new Object[(int) v.size()];
        int i = 0;
        for(Value key: v) {
            keys[i] = key.toJava();
            i++;
        }
        ComplexKey k = ComplexKey.of(keys);
        return k;
    }
    protected String javaMapToJson(final java.util.Map<String, Object> map) {
        final StringBuilder json = new StringBuilder();
        if(map.size() > 1)
            json.append("[ ");
         else
             json.append("{ ");
        for(String key: map.keySet()) {
            Object value = map.get(key);
            if(value instanceof java.util.Map<?, ?>) {
                json.append(key).append(" : ").append(javaMapToJson(map));
            } else {
                json.append(key).append(" : ").append(value);
            }
        }
        if(map.size() > 1)
            json.append("] ");
         else
             json.append("} ");
        return json.toString();
    }
    /**
     * create Json format string from view Response.
     * @param viewResponse
     * @return
     */
    protected Str viewResponseToJson(final ViewResponse viewResponse) {
       //return Str.get(javaMapToJson(viewResponse.getMap()));
        final StringBuilder json = new StringBuilder();
        json.append("{ ");
        for (ViewRow v: viewResponse) {
            if(json.length() > 2) json.append(", ");
            json.append(v.getKey()).append(" : ");
            String value = v.getValue();
            if(value != null) {
                value = value.trim();
                if(value.charAt(0) == '{' || value.charAt(0) == '[') {
                    json.append(v.getValue());
                } else {
                    json.append('"').append(value.replaceAll("\"", "\\\"")).
       append('"');;
                }
            } else {
                json.append('"').append("").append('"');
            }
        }
        json.append(" } ");
        return Str.get(json.toString());
    }
    /**
     * create Json format string from view Response.
     * @param viewResponse
     * @return
     */
    protected Str viewResponseToJsonValueOnly(final ViewResponse viewResponse) {
        final StringBuilder json = new StringBuilder();
        if(viewResponse.size() > 1) {
            json.append("[ ");
        }
        for (ViewRow v: viewResponse) {
            if(json.length() > 2) json.append(", ");
            //json.append('"').append(v.getKey()).append('"').append(" : ");
            String value = v.getValue();
            if(value != null) {
                value = value.trim();
                if(value.charAt(0) == '{' || value.charAt(0) == '[') {
                    json.append(v.getValue());
                } else {
                    json.append('"').append(value.replaceAll("\"", "\\\"")).append('"');;
                }
            } else {
                json.append('"').append("").append('"');
            }
        }
        if(viewResponse.size() > 1) {
            json.append(" ] ");
        }
        return Str.get(json.toString());
    }
    /**
     *  close database instanses.
     * @param handler
     * @throws QueryException
     */
    public void shutdown(final Str handler) throws QueryException {
        shutdown(handler, null);
    }
    /**
     *  close database connection after certain time.
     * @param handler
     * @param time in seconds
     * @throws QueryException
     */
    public void shutdown(final Str handler, final Item time)
            throws QueryException {
        CouchbaseClient client = getClient(handler);
        if(time != null) {
            if(!time.type().instanceOf(SeqType.ITR)) {
                throw CouchbaseErrors.timeInvalid();
            }
            long seconds = ((Item) time).itr(null);
            boolean result = client.shutdown(seconds, TimeUnit.SECONDS);
            if(!result) {
                throw CouchbaseErrors.shutdownError();
            }
        } else {
            client.shutdown();
        }
    }
    /**
     * Convert viewresponse to JSON in pattern of Key Value like {key:value} or
     * [{key:value},{key:value}..].
     * @param vr Couchbase {@link ViewResponse}
     * @return String
     * @throws JSONException
     */
    protected String vrTOJson(final ViewResponse vr) throws QueryException {
        try {
            JSONArray j = new JSONArray();
            int size = vr.size();
            if(size > 0) {
               for(ViewRow r: vr) {
                JSONObject jo = new JSONObject();
                jo.put(r.getKey(), r.getValue());
                if(size == 1)
                    return jo.toString();
                j.put(jo);
               }
               return j.toString();
           }
        } catch (JSONException e) {
            CouchbaseErrors.generalExceptionError(e);
        }
       return null;
    }
}
